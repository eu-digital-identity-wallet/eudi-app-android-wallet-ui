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

package eu.europa.ec.startupfeature.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.utils.screenPaddings
import eu.europa.ec.uilogic.component.wrap.WrapPrimaryButton
import eu.europa.ec.uilogic.navigation.ModuleRoute
import eu.europa.ec.uilogic.navigation.StartupScreens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun StartupScreen(
    navController: NavController,
    viewModel: StartupViewModel
) {
    Content(
        state = viewModel.viewState.value,
        effectFlow = viewModel.effect,
        onEventSend = { viewModel.setEvent(it) },
        onNavigationRequested = {
            when (it) {
                is Effect.Navigation.SwitchModule -> {
                    navController.navigate(it.moduleRoute.route) {
                        popUpTo(ModuleRoute.StartupModule.route) { inclusive = true }
                    }
                }

                is Effect.Navigation.SwitchScreen -> {
                    navController.navigate(it.screen) {
                        popUpTo(StartupScreens.Splash.screenRoute) { inclusive = true }
                    }
                }
            }
        }
    )
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>?,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(screenPaddings()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WrapPrimaryButton(
            onClick = { onEventSend(Event.OnClick) }
        ) {
            Text(text = "Press here to test")
        }

        VSpacer.Medium()

        WrapPrimaryButton(
            onClick = { onEventSend(Event.OnlineAuthenticationClicked) }
        ) {
            Text(text = "ONLINE AUTHENTICATION")
        }
    }

    LaunchedEffect(Unit) {
        effectFlow?.onEach { effect ->
            when (effect) {
                is Effect.Navigation.SwitchModule -> onNavigationRequested(effect)
                is Effect.Navigation.SwitchScreen -> onNavigationRequested(effect)
            }
        }?.collect()
    }
}