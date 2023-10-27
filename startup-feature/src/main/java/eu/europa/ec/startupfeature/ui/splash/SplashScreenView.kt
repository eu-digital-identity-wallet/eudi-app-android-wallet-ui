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

package eu.europa.ec.startupfeature.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.wrap.WrapImage
import eu.europa.ec.uilogic.navigation.ModuleRoute
import eu.europa.ec.uilogic.navigation.StartupScreens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach


@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashScreenViewModel
) {
    Content(
        state = viewModel.viewState.value,
        effectFlow = viewModel.effect,
        onNavigationRequested = {
            when (it) {
                is Effect.Navigation.SwitchModule -> {
                    navController.navigate(it.moduleRoute.route) {
                        popUpTo(ModuleRoute.StartupModule.route) { inclusive = true }
                    }
                }

                is Effect.Navigation.SwitchScreen -> {
                    navController.navigate(it.screen.screenRoute) {
                        popUpTo(StartupScreens.Splash.screenRoute) { inclusive = true }
                    }
                }
            }
        }
    )

    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.Initialize)
    }
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>?,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit
) {
    val visibilityState = remember {
        MutableTransitionState(false).apply {
            targetState = true
        }
    }
    Scaffold { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visibleState = visibilityState,
                enter = fadeIn(animationSpec = tween(state.logoAnimationDuration)),
                exit = fadeOut(animationSpec = tween(state.logoAnimationDuration)),
            ) {
                WrapImage(
                    iconData = AppIcons.Logo
                )
            }

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