/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.dashboardfeature.ui.dashboard

import android.content.Intent
import android.net.Uri
import eu.europa.ec.businesslogic.extension.toUri
import eu.europa.ec.commonfeature.config.OfferUiConfig
import eu.europa.ec.commonfeature.config.PresentationMode
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.commonfeature.model.PinFlow
import eu.europa.ec.corelogic.di.getOrCreatePresentationScope
import eu.europa.ec.dashboardfeature.interactor.DashboardInteractor
import eu.europa.ec.dashboardfeature.model.SideMenuItemType
import eu.europa.ec.dashboardfeature.model.SideMenuItemUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.helper.DeepLinkType
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.navigation.helper.hasDeepLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import org.koin.android.annotation.KoinViewModel

data class State(

    // side menu
    val isSideMenuVisible: Boolean = false,
    val sideMenuTitle: String = "",
    val sideMenuOptions: List<SideMenuItemUi>,
    val sideMenuAnimation: SideMenuAnimation = SideMenuAnimation.SLIDE,
    val menuAnimationDuration: Int = 1500,
    val appVersion: String = "",
    val changelogUrl: String?,
) : ViewState

sealed class Event : ViewEvent {
    data class Init(val deepLinkUri: Uri?) : Event()
    data object Pop : Event()

    // side menu events
    sealed class SideMenu : Event() {
        data object Show : SideMenu()
        data object Hide : SideMenu()
        data class ItemClicked(val itemType: SideMenuItemType) : SideMenu()
    }
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object Pop : Navigation()
        data class SwitchScreen(
            val screenRoute: String,
            val popUpToScreenRoute: String = DashboardScreens.Dashboard.screenRoute,
            val inclusive: Boolean = false,
        ) : Navigation()

        data class OpenDeepLinkAction(val deepLinkUri: Uri, val arguments: String?) :
            Navigation()

        data object OnAppSettings : Navigation()
        data object OnSystemSettings : Navigation()
        data class OpenUrlExternally(val url: Uri) : Navigation()
    }

    data class ShareLogFile(val intent: Intent, val chooserTitle: String) : Effect()
}

enum class SideMenuAnimation {
    SLIDE, FADE
}

@KoinViewModel
class DashboardViewModel(
    private val dashboardInteractor: DashboardInteractor,
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
) : MviViewModel<Event, State, Effect>() {
    override fun setInitialState(): State {
        val changelogUrl = dashboardInteractor.getChangelogUrl()
        return State(
            sideMenuTitle = resourceProvider.getString(R.string.dashboard_side_menu_title),
            sideMenuOptions = getSideMenuOptions(changelogUrl = changelogUrl),
            appVersion = dashboardInteractor.getAppVersion(),
            changelogUrl = changelogUrl,
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> handleDeepLink(event.deepLinkUri)

            is Event.Pop -> setEffect { Effect.Navigation.Pop }

            is Event.SideMenu.ItemClicked -> {
                handleSideMenuItemClicked(event.itemType)
            }

            is Event.SideMenu.Hide -> {
                setState {
                    copy(
                        isSideMenuVisible = false,
                        sideMenuAnimation = SideMenuAnimation.SLIDE
                    )
                }
            }

            is Event.SideMenu.Show -> {
                setState {
                    copy(
                        isSideMenuVisible = true,
                        sideMenuAnimation = SideMenuAnimation.SLIDE
                    )
                }
            }
        }
    }

    private fun handleSideMenuItemClicked(itemType: SideMenuItemType) {
        when (itemType) {
            SideMenuItemType.CHANGE_PIN -> {
                val nextScreenRoute = generateComposableNavigationLink(
                    screen = CommonScreens.QuickPin,
                    arguments = generateComposableArguments(
                        mapOf("pinFlow" to PinFlow.UPDATE)
                    )
                )

                setState {
                    copy(
                        isSideMenuVisible = false,
                        sideMenuAnimation = SideMenuAnimation.FADE
                    )
                }
                setEffect { Effect.Navigation.SwitchScreen(screenRoute = nextScreenRoute) }
            }

            SideMenuItemType.RETRIEVE_LOGS -> {
                val logs = dashboardInteractor.retrieveLogFileUris()
                if (logs.isNotEmpty()) {
                    setEffect {
                        Effect.ShareLogFile(
                            intent = Intent().apply {
                                action = Intent.ACTION_SEND_MULTIPLE
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, logs)
                                type = "text/*"
                            },
                            chooserTitle = resourceProvider.getString(R.string.dashboard_intent_chooser_logs_share_title)
                        )
                    }
                }
            }

            SideMenuItemType.CHANGELOG -> {
                val changelogUrl = viewState.value.changelogUrl
                if (changelogUrl != null) {
                    setEffect {
                        Effect.Navigation.OpenUrlExternally(
                            url = changelogUrl.toUri()
                        )
                    }
                }
            }
        }
    }

    private fun getSideMenuOptions(changelogUrl: String?): List<SideMenuItemUi> {
        return buildList {
            add(
                SideMenuItemUi(
                    type = SideMenuItemType.CHANGE_PIN,
                    data = ListItemData(
                        itemId = resourceProvider.getString(R.string.dashboard_side_menu_change_pin_id),
                        mainContentData = ListItemMainContentData.Text(
                            text = resourceProvider.getString(R.string.dashboard_side_menu_change_pin)
                        ),
                        leadingContentData = ListItemLeadingContentData.Icon(
                            iconData = AppIcons.ChangePin
                        ),
                        trailingContentData = ListItemTrailingContentData.Icon(
                            iconData = AppIcons.KeyboardArrowRight
                        )
                    )
                )
            )
            add(
                SideMenuItemUi(
                    type = SideMenuItemType.RETRIEVE_LOGS,
                    data = ListItemData(
                        itemId = resourceProvider.getString(R.string.dashboard_side_menu_retrieve_logs_id),
                        mainContentData = ListItemMainContentData.Text(
                            text = resourceProvider.getString(R.string.dashboard_side_menu_retrieve_logs)
                        ),
                        leadingContentData = ListItemLeadingContentData.Icon(
                            iconData = AppIcons.OpenNew
                        ),
                        trailingContentData = ListItemTrailingContentData.Icon(
                            iconData = AppIcons.KeyboardArrowRight
                        )
                    )
                )
            )
            if (changelogUrl != null) {
                add(
                    SideMenuItemUi(
                        type = SideMenuItemType.CHANGELOG,
                        data = ListItemData(
                            itemId = resourceProvider.getString(R.string.dashboard_side_menu_changelog_id),
                            mainContentData = ListItemMainContentData.Text(
                                text = resourceProvider.getString(R.string.dashboard_side_menu_changelog)
                            ),
                            leadingContentData = ListItemLeadingContentData.Icon(
                                iconData = AppIcons.OpenInBrowser
                            ),
                            trailingContentData = ListItemTrailingContentData.Icon(
                                iconData = AppIcons.KeyboardArrowRight
                            )
                        )
                    )
                )
            }
        }
    }

    private fun handleDeepLink(deepLinkUri: Uri?) {
        deepLinkUri?.let { uri ->
            hasDeepLink(uri)?.let {
                val arguments: String? = when (it.type) {
                    DeepLinkType.OPENID4VP -> {
                        getOrCreatePresentationScope()
                        generateComposableArguments(
                            mapOf(
                                RequestUriConfig.serializedKeyName to uiSerializer.toBase64(
                                    RequestUriConfig(
                                        PresentationMode.OpenId4Vp(
                                            uri.toString(),
                                            DashboardScreens.Dashboard.screenRoute
                                        )
                                    ),
                                    RequestUriConfig.Parser
                                )
                            )
                        )
                    }

                    DeepLinkType.CREDENTIAL_OFFER -> generateComposableArguments(
                        mapOf(
                            OfferUiConfig.serializedKeyName to uiSerializer.toBase64(
                                OfferUiConfig(
                                    offerURI = it.link.toString(),
                                    onSuccessNavigation = ConfigNavigation(
                                        navigationType = NavigationType.PopTo(
                                            screen = DashboardScreens.Dashboard
                                        )
                                    ),
                                    onCancelNavigation = ConfigNavigation(
                                        navigationType = NavigationType.Pop
                                    )
                                ),
                                OfferUiConfig.Parser
                            )
                        )
                    )

                    else -> null
                }
                setEffect {
                    Effect.Navigation.OpenDeepLinkAction(
                        deepLinkUri = uri,
                        arguments = arguments
                    )
                }
            }
        }
    }
}