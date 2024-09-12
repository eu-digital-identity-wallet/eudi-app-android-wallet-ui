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

package eu.europa.ec.issuancefeature.ui.document.add

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.model.DocumentOptionItemUi
import eu.europa.ec.corelogic.controller.IssuanceMethod
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.allCorneredShapeSmall
import eu.europa.ec.resourceslogic.theme.values.backgroundDefault
import eu.europa.ec.resourceslogic.theme.values.textSecondaryDark
import eu.europa.ec.resourceslogic.theme.values.topCorneredShapeSmall
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.IconData
import eu.europa.ec.uilogic.component.IssuanceButton
import eu.europa.ec.uilogic.component.IssuanceButtonData
import eu.europa.ec.uilogic.component.SystemBroadcastReceiver
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.extension.finish
import eu.europa.ec.uilogic.extension.getPendingDeepLink
import eu.europa.ec.uilogic.extension.throttledClickable
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.helper.handleDeepLinkAction
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun AddDocumentScreen(
    navController: NavController,
    viewModel: AddDocumentViewModel
) {
    val state = viewModel.viewState.value
    val context = LocalContext.current

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = state.navigatableAction,
        onBack = state.onBackAction,
        contentErrorConfig = state.error
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                when (navigationEffect) {
                    is Effect.Navigation.Pop -> navController.popBackStack()
                    is Effect.Navigation.SwitchScreen -> {
                        navController.navigate(navigationEffect.screenRoute) {
                            popUpTo(IssuanceScreens.AddDocument.screenRoute) {
                                inclusive = navigationEffect.inclusive
                            }
                        }
                    }

                    is Effect.Navigation.Finish -> context.finish()
                    is Effect.Navigation.OpenDeepLinkAction -> handleDeepLinkAction(
                        navController,
                        navigationEffect.deepLinkUri,
                        navigationEffect.arguments
                    )
                }
            },
            paddingValues = paddingValues,
            context = context
        )
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

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(paddingValues)
        ) {
            ContentTitle(
                title = state.title,
                subtitle = state.subtitle
            )

            LazyColumn {
                state.options.forEach { option ->
                    item {
                        IssuanceButton(
                            data = IssuanceButtonData(
                                text = option.text,
                                icon = option.icon
                            ),
                            enabled = option.available,
                            onClick = {
                                onEventSend(
                                    Event.IssueDocument(
                                        issuanceMethod = IssuanceMethod.OPENID4VCI,
                                        documentType = option.type.docType,
                                        context = context
                                    )
                                )
                            }
                        )

                        VSpacer.Medium()
                    }
                }
            }
        }

        QrScanSection(
            onClick = { onEventSend(Event.GoToQrScan) }
        )
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)
            }
        }.collect()
    }
}

@Composable
private fun QrScanSection(
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                color = MaterialTheme.colorScheme.backgroundDefault.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.topCorneredShapeSmall
            )
            .padding(
                top = SPACING_EXTRA_LARGE.dp,
                bottom = SPACING_LARGE.dp,
                start = SPACING_LARGE.dp,
                end = SPACING_LARGE.dp,
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.issuance_add_document_qr_scan_section_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textSecondaryDark
        )
        VSpacer.Medium()
        InnerSection(
            icon = AppIcons.QR,
            text = stringResource(id = R.string.issuance_add_document_qr_scan_section_subtitle),
            onClick = onClick
        )
    }
}

@Composable
private fun InnerSection(
    icon: IconData,
    text: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(shape = MaterialTheme.shapes.allCorneredShapeSmall)
            .background(
                color = MaterialTheme.colorScheme.backgroundDefault
            )
            .throttledClickable(onClick = onClick)
            .padding(vertical = SPACING_MEDIUM.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WrapIcon(
            iconData = icon,
            modifier = Modifier.size(40.dp),
            customTint = MaterialTheme.colorScheme.primary
        )
        VSpacer.Small()
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@ThemeModePreviews
@Composable
private fun IssuanceAddDocumentScreenPreview() {
    PreviewTheme {
        Content(
            state = State(
                navigatableAction = ScreenNavigateAction.NONE,
                title = "Add document",
                subtitle = "Select a document to add in your EUDI Wallet",
                options = listOf(
                    DocumentOptionItemUi(
                        text = "National ID",
                        icon = AppIcons.Id,
                        type = DocumentIdentifier.PID,
                        available = true,
                    ),
                    DocumentOptionItemUi(
                        text = "Driving License",
                        icon = AppIcons.Id,
                        type = DocumentIdentifier.MDL,
                        available = false,
                    )
                )
            ),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(all = SPACING_LARGE.dp),
            context = LocalContext.current
        )
    }
}

@ThemeModePreviews
@Composable
private fun DashboardAddDocumentScreenPreview() {
    PreviewTheme {
        Content(
            state = State(
                navigatableAction = ScreenNavigateAction.CANCELABLE,
                title = "Add document",
                subtitle = "Select a document to add in your EUDI Wallet",
                options = listOf(
                    DocumentOptionItemUi(
                        text = "National ID",
                        icon = AppIcons.Id,
                        type = DocumentIdentifier.PID,
                        available = true,
                    ),
                    DocumentOptionItemUi(
                        text = "Driving License",
                        icon = AppIcons.Id,
                        type = DocumentIdentifier.MDL,
                        available = false,
                    )
                )
            ),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(all = SPACING_LARGE.dp),
            context = LocalContext.current
        )
    }
}