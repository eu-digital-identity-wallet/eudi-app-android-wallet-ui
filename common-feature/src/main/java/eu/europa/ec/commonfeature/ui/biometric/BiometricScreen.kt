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

package eu.europa.ec.commonfeature.ui.biometric

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.config.BiometricUiConfig
import eu.europa.ec.commonfeature.config.OnBackNavigationConfig
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SIZE_MEDIUM
import eu.europa.ec.uilogic.component.wrap.WrapIconButton
import eu.europa.ec.uilogic.component.wrap.WrapPinTextField
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.FlowCompletion
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.extension.cacheDeepLink
import eu.europa.ec.uilogic.extension.finish
import eu.europa.ec.uilogic.extension.resetBackStack
import eu.europa.ec.uilogic.extension.setBackStackFlowCancelled
import eu.europa.ec.uilogic.extension.setBackStackFlowSuccess
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.helper.handleDeepLinkAction
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun BiometricScreen(
    navController: NavController,
    viewModel: BiometricViewModel
) {
    val state = viewModel.viewState.value
    val context = LocalContext.current

    ContentScreen(
        loadingType = state.isLoading,
        navigatableAction = if (state.isCancellable) {
            ScreenNavigateAction.CANCELABLE
        } else {
            ScreenNavigateAction.NONE
        },
        onBack = {
            viewModel.setEvent(Event.OnNavigateBack)
        },
        contentErrorConfig = state.error
    ) {
        Body(
            state = state,
            effectFlow = viewModel.effect,
            onEventSent = { event -> viewModel.setEvent(event) },
            onNavigationRequested = { navigationEffect ->
                when (navigationEffect) {
                    is Effect.Navigation.SwitchScreen -> {
                        navController.navigate(navigationEffect.screen) {
                            popUpTo(CommonScreens.Biometric.screenRoute) { inclusive = true }
                        }
                    }

                    is Effect.Navigation.LaunchBiometricsSystemScreen -> {
                        viewModel.setEvent(Event.LaunchBiometricSystemScreen)
                    }

                    is Effect.Navigation.PopBackStackUpTo -> {
                        when (navigationEffect.indicateFlowCompletion) {
                            FlowCompletion.CANCEL -> {
                                navController.setBackStackFlowCancelled(
                                    navigationEffect.screenRoute
                                )
                            }

                            FlowCompletion.SUCCESS -> {
                                navController.setBackStackFlowSuccess(
                                    navigationEffect.screenRoute
                                )
                            }

                            FlowCompletion.NONE -> {
                                navController.resetBackStack(
                                    navigationEffect.screenRoute
                                )
                            }
                        }
                        navController.popBackStack(
                            route = navigationEffect.screenRoute,
                            inclusive = navigationEffect.inclusive
                        )
                    }

                    is Effect.Navigation.Deeplink -> {
                        navigationEffect.routeToPop?.let { route ->
                            context.cacheDeepLink(navigationEffect.link)
                            if (navigationEffect.isPreAuthorization) {
                                navController.navigate(route) {
                                    popUpTo(CommonScreens.Biometric.screenRoute) {
                                        inclusive = true
                                    }
                                }
                            } else {
                                navController.popBackStack(
                                    route = route,
                                    inclusive = false
                                )
                            }
                        } ?: handleDeepLinkAction(navController, navigationEffect.link)

                    }

                    is Effect.Navigation.Pop -> navController.popBackStack()
                    is Effect.Navigation.Finish -> context.finish()
                }
            },
            padding = it
        )
    }

    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.Init)
    }
}

@Composable
private fun Body(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSent: ((event: Event) -> Unit),
    onNavigationRequested: ((navigationEffect: Effect.Navigation) -> Unit),
    padding: PaddingValues
) {

    // Get application context.
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.Center
    ) {
        ContentTitle(
            title = state.config.title,
            subtitle = if (state.userBiometricsAreEnabled) {
                state.config.subTitle
            } else {
                state.config.quickPinOnlySubTitle
            }
        )
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopStart
        ) {
            PinFieldLayout(state) { quickPin ->
                onEventSent(
                    Event.OnQuickPinEntered(
                        quickPin
                    )
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            if (state.userBiometricsAreEnabled) {
                WrapIconButton(
                    iconData = AppIcons.TouchId,
                    modifier = Modifier.padding(bottom = 5.dp),
                    onClick = {
                        onEventSent(
                            Event.OnBiometricsClicked(
                                context = context,
                                shouldThrowErrorIfNotAvailable = true
                            )
                        )
                    }
                )
            }
        }

        LaunchedEffect(Unit) {
            effectFlow.onEach { effect ->
                when (effect) {
                    is Effect.Navigation -> {
                        onNavigationRequested(effect)
                    }

                    is Effect.InitializeBiometricAuthOnCreate -> {
                        onEventSent(
                            Event.OnBiometricsClicked(
                                context,
                                false
                            )
                        )
                    }
                }
            }.collect()
        }
    }
}

@Composable
private fun PinFieldLayout(
    state: State,
    onPinInput: (String) -> Unit,
) {
    WrapPinTextField(
        onPinUpdate = {
            onPinInput(it)
        },
        length = state.quickPinSize,
        hasError = !state.quickPinError.isNullOrEmpty(),
        errorMessage = state.quickPinError,
        visualTransformation = PasswordVisualTransformation(),
        pinWidth = 46.dp,
        focusOnCreate = !state.userBiometricsAreEnabled
    )
}

/**
 * Preview composable of [Body].
 */
@ThemeModePreviews
@Composable
private fun PreviewBiometricScreen() {
    PreviewTheme {
        Body(
            state = State(
                config = BiometricUiConfig(
                    title = "Biometric Title",
                    subTitle = "Biometric Subtitle",
                    quickPinOnlySubTitle = "Quick Pin Subtitle",
                    isPreAuthorization = true,
                    onSuccessNavigation = ConfigNavigation(
                        navigationType = NavigationType.PushScreen(CommonScreens.Biometric)
                    ),
                    onBackNavigationConfig = OnBackNavigationConfig(
                        onBackNavigation = ConfigNavigation(
                            navigationType = NavigationType.PushScreen(CommonScreens.Biometric),
                        ),
                        hasToolbarCancelIcon = true
                    )
                )
            ),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSent = {},
            onNavigationRequested = {},
            padding = PaddingValues(SIZE_MEDIUM.dp)
        )
    }
}