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

package eu.europa.ec.issuancefeature.ui.code

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.config.OfferCodeUiConfig
import eu.europa.ec.uilogic.component.AppIconAndText
import eu.europa.ec.uilogic.component.AppIconAndTextData
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.WrapPinTextField
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun DocumentOfferCodeScreen(
    navController: NavController,
    viewModel: DocumentOfferCodeViewModel
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(Event.Pop) },
    ) { paddingValues ->
        Content(
            context = context,
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(navigationEffect, navController)
            },
            paddingValues = paddingValues
        )
    }
}

@Composable
private fun Content(
    context: Context,
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        AppIconAndText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SPACING_LARGE.dp),
            appIconAndTextData = AppIconAndTextData(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SPACING_LARGE.dp),
            verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp, Alignment.Top)
        ) {
            Text(
                text = state.screenTitle,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Text(
                text = state.screenSubtitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SPACING_LARGE.dp),
            verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp, Alignment.Top)
        ) {
            CodeFieldLayout(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                onPinInput = { quickPin ->
                    onEventSend(
                        Event.OnPinChange(
                            code = quickPin,
                            context = context
                        )
                    )
                }
            )
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

@Composable
private fun CodeFieldLayout(
    modifier: Modifier,
    state: State,
    onPinInput: (String) -> Unit,
) {
    WrapPinTextField(
        modifier = modifier,
        onPinUpdate = {
            onPinInput(it)
        },
        length = state.offerCodeUiConfig.txCodeLength,
        visualTransformation = PasswordVisualTransformation(),
        pinWidth = 42.dp,
        focusOnCreate = true,
        shouldHideKeyboardOnCompletion = true
    )
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController
) {
    when (navigationEffect) {
        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute) {
                popUpTo(IssuanceScreens.DocumentOfferCode.screenRoute) {
                    inclusive = true
                }
            }
        }

        is Effect.Navigation.Pop -> navController.popBackStack()
    }
}

@ThemeModePreviews
@Composable
private fun DocumentOfferCodeScreenEmptyPreview() {
    PreviewTheme {
        Content(
            state = State(
                isLoading = false,
                error = null,
                notifyOnAuthenticationFailure = false,
                screenTitle = "Demo Issuer requires verification",
                screenSubtitle = "Type the 5-digit transaction code you received.",
                offerCodeUiConfig = OfferCodeUiConfig(
                    offerURI = "https://offer.uri.com",
                    txCodeLength = 5,
                    issuerName = "Demo Issuer",
                    onSuccessNavigation = ConfigNavigation(
                        navigationType = NavigationType.Pop
                    )
                )
            ),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(SPACING_MEDIUM.dp),
            context = LocalContext.current
        )
    }
}