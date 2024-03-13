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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.model.DocumentOptionItemUi
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.corelogic.controller.IssuanceMethod
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.IssuanceButton
import eu.europa.ec.uilogic.component.IssuanceButtonData
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.navigation.IssuanceScreens
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
                }
            },
            paddingValues = paddingValues,
            context = context
        )
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME
    ) {
        viewModel.setEvent(Event.Init)
    }
}

@Composable
fun Content(
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
            .padding(paddingValues),
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

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)
            }
        }.collect()
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
                        type = DocumentTypeUi.PID,
                        available = true,
                    ),
                    DocumentOptionItemUi(
                        text = "Driving License",
                        icon = AppIcons.Id,
                        type = DocumentTypeUi.MDL,
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
                        type = DocumentTypeUi.PID,
                        available = true,
                    ),
                    DocumentOptionItemUi(
                        text = "Driving License",
                        icon = AppIcons.Id,
                        type = DocumentTypeUi.MDL,
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