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

package eu.europa.ec.issuancefeature.ui.document.offer

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.config.OfferUiConfig
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.ErrorInfo
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.MainContentData
import eu.europa.ec.uilogic.component.RelyingPartyData
import eu.europa.ec.uilogic.component.SystemBroadcastReceiver
import eu.europa.ec.uilogic.component.content.ContentHeader
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextData
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.DialogBottomSheet
import eu.europa.ec.uilogic.component.wrap.StickyBottomConfig
import eu.europa.ec.uilogic.component.wrap.StickyBottomType
import eu.europa.ec.uilogic.component.wrap.WrapListItems
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapStickyBottomContent
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.extension.cacheDeepLink
import eu.europa.ec.uilogic.extension.getPendingDeepLink
import eu.europa.ec.uilogic.navigation.DashboardScreens
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.helper.handleDeepLinkAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentOfferScreen(
    navController: NavController,
    viewModel: DocumentOfferViewModel
) {
    val state = viewModel.viewState.value
    val context = LocalContext.current

    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(Event.BackButtonPressed) },
        stickyBottom = { paddingValues ->
            WrapStickyBottomContent(
                stickyBottomModifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues),
                stickyBottomConfig = StickyBottomConfig(
                    type = StickyBottomType.OneButton(
                        config = ButtonConfig(
                            type = ButtonType.PRIMARY,
                            enabled = !state.isLoading && !state.noDocument,
                            onClick = { viewModel.setEvent(Event.PrimaryButtonPressed(context)) }
                        )
                    )
                )
            ) {
                Text(text = stringResource(R.string.issuance_document_offer_primary_button_text_add))
            }
        }
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(context, navigationEffect, navController)
            },
            paddingValues = paddingValues,
            coroutineScope = scope,
            modalBottomSheetState = bottomSheetState,
        )

        if (isBottomSheetOpen) {
            WrapModalBottomSheet(
                onDismissRequest = {
                    viewModel.setEvent(
                        Event.BottomSheet.UpdateBottomSheetState(
                            isOpen = false
                        )
                    )
                },
                sheetState = bottomSheetState
            ) {
                SheetContent(
                    onEventSent = {
                        viewModel.setEvent(it)
                    }
                )
            }
        }
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_PAUSE
    ) {
        viewModel.setEvent(Event.OnPause)
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME
    ) {
        viewModel.setEvent(Event.Init(context.getPendingDeepLink()))
    }

    SystemBroadcastReceiver(
        actions = listOf(
            CoreActions.VCI_RESUME_ACTION,
            CoreActions.VCI_DYNAMIC_PRESENTATION
        )
    ) {
        when (it?.action) {
            CoreActions.VCI_RESUME_ACTION -> it.extras?.getString("uri")?.let { link ->
                viewModel.setEvent(Event.OnResumeIssuance(link))
            }

            CoreActions.VCI_DYNAMIC_PRESENTATION -> it.extras?.getString("uri")?.let { link ->
                viewModel.setEvent(Event.OnDynamicPresentation(link))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
    coroutineScope: CoroutineScope,
    modalBottomSheetState: SheetState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (state.isLoading.not()) {
            if (state.noDocument) {
                ErrorInfo(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    informativeText = stringResource(id = R.string.issuance_document_offer_error_no_document)
                )
            } else {
                ContentHeader(
                    modifier = Modifier.fillMaxWidth(),
                    config = state.headerConfig,
                )

                // Screen Main Content
                MainContent(
                    documents = state.documents,
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)

                is Effect.CloseBottomSheet -> {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                    }.invokeOnCompletion {
                        if (!modalBottomSheetState.isVisible) {
                            onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                        }
                    }
                }

                is Effect.ShowBottomSheet -> {
                    onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
                }
            }
        }.collect()
    }
}

@Composable
private fun SheetContent(
    onEventSent: (event: Event) -> Unit,
) {
    DialogBottomSheet(
        textData = BottomSheetTextData(
            title = stringResource(id = R.string.issuance_document_offer_bottom_sheet_cancel_title),
            message = stringResource(id = R.string.issuance_document_offer_bottom_sheet_cancel_subtitle),
            positiveButtonText = stringResource(id = R.string.issuance_document_offer_bottom_sheet_cancel_primary_button_text),
            negativeButtonText = stringResource(id = R.string.issuance_document_offer_bottom_sheet_cancel_secondary_button_text),
        ),
        onPositiveClick = { onEventSent(Event.BottomSheet.Cancel.PrimaryButtonPressed) },
        onNegativeClick = { onEventSent(Event.BottomSheet.Cancel.SecondaryButtonPressed) }
    )
}

@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    documents: List<ListItemData>,
) {
    VSpacer.Small()

    WrapListItems(
        modifier = modifier,
        items = documents,
        onItemClick = null,
        mainContentVerticalPadding = SPACING_LARGE.dp
    )
}

private fun handleNavigationEffect(
    context: Context,
    navigationEffect: Effect.Navigation,
    navController: NavController
) {
    when (navigationEffect) {
        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute) {
                if (navigationEffect.shouldPopToSelf) {
                    popUpTo(IssuanceScreens.DocumentOffer.screenRoute) {
                        inclusive = true
                    }
                }
            }
        }

        is Effect.Navigation.PopBackStackUpTo -> {
            navController.popBackStack(
                route = navigationEffect.screenRoute,
                inclusive = navigationEffect.inclusive
            )
        }

        is Effect.Navigation.DeepLink -> {
            navigationEffect.routeToPop?.let {
                context.cacheDeepLink(navigationEffect.link)
                navController.popBackStack(
                    route = it,
                    inclusive = false
                )
            } ?: handleDeepLinkAction(navController, navigationEffect.link)
        }

        is Effect.Navigation.Pop -> navController.popBackStack()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun ContentPreview() {
    PreviewTheme {
        val previewState = State(
            isLoading = false,
            error = null,
            isBottomSheetOpen = false,
            isInitialised = true,
            issuerName = "Placeholder Issuer",
            documents = listOf(
                ListItemData(
                    itemId = "doc_1",
                    mainContentData = MainContentData.Text("PID")
                )
            ),
            noDocument = false,
            headerConfig = ContentHeaderConfig(
                description = "",
                mainText = "",
                relyingPartyData = RelyingPartyData(
                    isVerified = true,
                    name = "Placeholder Relying Party",
                    description = ""
                )
            ),
            offerUiConfig = OfferUiConfig(
                offerURI = "",
                onSuccessNavigation = ConfigNavigation(
                    navigationType = NavigationType.PushScreen(
                        screen = DashboardScreens.Dashboard,
                        popUpToScreen = IssuanceScreens.AddDocument
                    )
                ),
                onCancelNavigation = ConfigNavigation(
                    navigationType = NavigationType.Pop
                )
            )
        )

        Content(
            state = previewState,
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(SPACING_MEDIUM.dp),
            coroutineScope = rememberCoroutineScope(),
            modalBottomSheetState = rememberModalBottomSheetState(),
        )
    }
}