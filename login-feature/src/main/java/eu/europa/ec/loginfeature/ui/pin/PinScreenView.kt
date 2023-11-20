/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.loginfeature.ui.pin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.wrap.WrapPinTextField
import eu.europa.ec.uilogic.component.wrap.WrapPrimaryButton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun PinScreen(
    navController: NavController,
    viewModel: PinViewModel,

    ) {
    val state = viewModel.viewState.value
    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = ScreenNavigateAction.NONE,
    ) { paddingValues ->
        Content(
            state = viewModel.viewState.value,
            effectFlow = viewModel.effect,
            onEventSent = { event -> viewModel.setEvent(event) },
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(navigationEffect, navController)
            },
            paddingValues = paddingValues,
        )
    }
    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.Init)
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController
) {
    when (navigationEffect) {
        is Effect.Navigation.SwitchScreen -> navController.navigate(navigationEffect.screen)
        is Effect.Navigation.SwitchModule -> navController.navigate(navigationEffect.moduleRoute.route)
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>?,
    onEventSent: ((Event) -> Unit),
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        if (state.isButtonEnabled) {
            keyboardController?.hide()
        }
        ContentTitle(
            title = state.title,
            subtitle = state.subtitle
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.align(alignment = Alignment.TopCenter)) {

                PinFieldLayout(state) { quickPin ->
                    onEventSent(
                        Event.OnQuickPinEntered(
                            quickPin
                        )
                    )
                }
            }

            WrapPrimaryButton(modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .fillMaxWidth(),
                enabled = state.isButtonEnabled,
                onClick = {
                    onEventSent(
                        Event.NextButtonPressed(
                            pin = state.pin
                        )
                    )
                }) {
                Text(text = stringResource(id = R.string.quick_pin_next_btn))
            }
        }
    }

    LaunchedEffect(Unit) {
        effectFlow?.onEach { effect ->
            when (effect) {
                is Effect.Navigation.SwitchModule -> onNavigationRequested(effect)
                is Effect.Navigation.SwitchScreen -> onNavigationRequested(effect)
                else -> {}
            }
        }?.collect()
    }
}

@Composable
fun PinFieldLayout(
    state: State,
    onPinInput: (String) -> Unit,
) {
    WrapPinTextField(
        onPinUpdate = {
            onPinInput(it)
        },
        length = 4,
        hasError = !state.quickPinError.isNullOrEmpty(),
        errorMessage = state.quickPinError,
        visualTransformation = PasswordVisualTransformation(),
        pinWidth = 46.dp,
        clearCode = state.resetPin
    )
}

@Preview(showSystemUi = true)
@Composable
private fun PinScreenEmptyPreview() {

    PreviewTheme {
        Content(
            state = State(),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSent = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(10.dp),
        )
    }
}