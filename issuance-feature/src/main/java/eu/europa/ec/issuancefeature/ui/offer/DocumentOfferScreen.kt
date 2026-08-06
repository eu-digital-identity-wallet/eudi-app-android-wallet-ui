/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.issuancefeature.ui.offer

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.config.OfferUiConfig
import eu.europa.ec.commonfeature.ui.issuance.IssuerNotTrustedSheetContent
import eu.europa.ec.commonfeature.ui.issuance.IssuerPartiallyTrustedSheetContent
import eu.europa.ec.commonfeature.ui.request.ConsentStickyBottomSection
import eu.europa.ec.commonfeature.ui.request.model.RelyingPartyHeaderUi
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.issuancefeature.util.TestTag
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.ErrorInfo
import eu.europa.ec.uilogic.component.InfoLinkSection
import eu.europa.ec.uilogic.component.InfoSection
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.RelyingParty
import eu.europa.ec.uilogic.component.RelyingPartyDataUi
import eu.europa.ec.uilogic.component.content.BroadcastAction
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.extension.cacheUri
import eu.europa.ec.uilogic.extension.getPendingUri
import eu.europa.ec.uilogic.extension.openUrl
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
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(Event.BackButtonPressed) },
        stickyBottom = { paddingValues ->
            ConsentStickyBottomSection(
                modifier = Modifier.fillMaxWidth(),
                paddingValues = paddingValues,
                buttonsTestTag = TestTag.DocumentOfferScreen.BUTTON,
                warningSection = null,
                primaryButtonText = stringResource(R.string.issuance_document_offer_accept_button_text),
                cancelButtonText = stringResource(R.string.issuance_document_offer_cancel_button_text),
                primaryButtonEnabled = !state.isLoading && state.allowAccept,
                onPrimaryButtonClick = { viewModel.setEvent(Event.StickyButtonPressed(context)) },
                onCancelButtonClick = { viewModel.setEvent(Event.BackButtonPressed) },
            )
        },
        broadcastAction = BroadcastAction(
            intentFilters = listOf(
                CoreActions.VCI_RESUME_ACTION,
                CoreActions.VCI_DYNAMIC_PRESENTATION
            ),
            callback = {
                when (it?.action) {
                    CoreActions.VCI_RESUME_ACTION -> it.extras?.getString("uri")?.let { link ->
                        viewModel.setEvent(Event.OnResumeIssuance(link))
                    }

                    CoreActions.VCI_DYNAMIC_PRESENTATION -> it.extras?.getString("uri")
                        ?.let { link ->
                            viewModel.setEvent(Event.OnDynamicPresentation(link))
                        }
                }
            }
        )
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
                    viewModel.setEvent(Event.BottomSheet.Close)
                },
                sheetState = bottomSheetState
            ) {
                SheetContent(
                    sheetContent = state.sheetContent,
                    onEventSent = { viewModel.setEvent(it) }
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
        viewModel.setEvent(Event.Init(context.getPendingUri()))
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
        // Screen Header.
        ContentTitle(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.issuance_document_offer_screen_title),
        )

        state.relyingPartyHeader?.let { safeRelyingPartyHeader ->
            IssuerHeaderSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_SMALL.dp),
                header = safeRelyingPartyHeader,
                onEventSend = onEventSend,
            )
        }

        if (state.noDocument) {
            ErrorInfo(
                modifier = Modifier.fillMaxSize(),
                informativeText = stringResource(id = R.string.issuance_document_offer_error_no_document)
            )
        } else {
            // Screen Main Content
            MainContent(
                modifier = Modifier.fillMaxSize(),
                documents = state.documents,
            )
        }
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)

                is Effect.ShowBottomSheet -> {
                    onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
                }

                is Effect.CloseBottomSheet -> {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                    }.invokeOnCompletion {
                        if (!modalBottomSheetState.isVisible) {
                            onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                            onEventSend(Event.BottomSheet.FinishedClosing)
                        }
                    }
                }
            }
        }.collect()
    }
}

@Composable
private fun SheetContent(
    sheetContent: DocumentOfferBottomSheetContent,
    onEventSent: (event: Event) -> Unit,
) {
    when (sheetContent) {
        is DocumentOfferBottomSheetContent.IssuerNotTrusted -> {
            IssuerNotTrustedSheetContent(
                onClose = {
                    onEventSent(Event.BottomSheet.Close)
                },
            )
        }

        is DocumentOfferBottomSheetContent.PartialSuccessWithUntrustedIssuer -> {
            IssuerPartiallyTrustedSheetContent(
                onClose = {
                    onEventSent(Event.BottomSheet.Close)
                },
            )
        }
    }
}

/**
 * The who-is-issuing header: the provider's identity and the verified registration sections
 * (privacy policy, intended use).
 */
@Composable
private fun IssuerHeaderSection(
    modifier: Modifier,
    header: RelyingPartyHeaderUi,
    onEventSend: (Event) -> Unit,
) {
    Column(modifier = modifier) {
        RelyingParty(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SPACING_SMALL.dp),
            relyingPartyData = header.relyingParty,
        )

        header.privacyPolicyUrl?.let { safePrivacyPolicyUrl ->
            InfoLinkSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_SMALL.dp),
                title = stringResource(R.string.request_privacy_policy_section_title),
                linkText = safePrivacyPolicyUrl,
                onLinkClick = { onEventSend(Event.PrivacyPolicyLinkClicked) },
            )
        }

        header.intendedUse?.let { safeIntendedUse ->
            InfoSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_SMALL.dp),
                title = stringResource(R.string.request_intended_use_section_title),
                body = safeIntendedUse,
            )
        }
    }
}

@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    documents: List<ListItemDataUi>,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = SPACING_SMALL.dp),
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
    ) {
        items(documents.size) { index ->
            WrapListItem(
                modifier = Modifier.fillMaxWidth(),
                item = documents[index],
                onItemClick = null,
                mainContentVerticalPadding = SPACING_LARGE.dp,
            )
        }
    }
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
                context.cacheUri(navigationEffect.link)
                navController.popBackStack(
                    route = it,
                    inclusive = false
                )
            } ?: handleDeepLinkAction(navController, navigationEffect.link)
        }

        is Effect.Navigation.Pop -> navController.popBackStack()

        is Effect.Navigation.OpenUrlExternally -> context.openUrl(uri = navigationEffect.url)
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
            isInitialised = true,
            documents = listOf(
                ListItemDataUi(
                    itemId = "doc_1",
                    mainContentData = ListItemMainContentDataUi.Text(text = "Boarding Pass")
                )
            ),
            noDocument = false,
            relyingPartyHeader = RelyingPartyHeaderUi(
                relyingParty = RelyingPartyDataUi(
                    logo = null,
                    isVerified = true,
                    name = "Aegean S.A.",
                    uniqueId = "rp:aegeanairlines:prod",
                    description = null,
                ),
                intendedUse = "Aegean Airlines is asking your permission to issue the " +
                        "following to your Wallet.",
                privacyPolicyUrl = "https://aegean.gr/privacy",
            ),
            offerUiConfig = OfferUiConfig(
                offerUri = "",
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