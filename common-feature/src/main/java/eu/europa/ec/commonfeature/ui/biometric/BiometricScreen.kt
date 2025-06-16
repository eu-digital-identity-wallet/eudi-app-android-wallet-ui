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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.config.BiometricMode
import eu.europa.ec.commonfeature.config.BiometricUiConfig
import eu.europa.ec.commonfeature.config.OnBackNavigationConfig
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.AppIconAndText
import eu.europa.ec.uilogic.component.AppIconAndTextDataUi
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.content.ContentHeader
import eu.europa.ec.uilogic.component.content.ContentHeaderConfig
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SIZE_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
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
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = if (state.isBackable) {
            ScreenNavigateAction.BACKABLE
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            MainContent(
                state = state,
                onEventSent = onEventSent,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
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
                                context = context,
                                shouldThrowErrorIfNotAvailable = false,
                            )
                        )
                    }
                }
            }.collect()
        }
    }
}

@Composable
private fun MainContent(
    state: State,
    onEventSent: (event: Event) -> Unit
) {
    when (val mode = state.config.mode) {
        is BiometricMode.Default -> {
            val description = if (state.userBiometricsAreEnabled) {
                mode.descriptionWhenBiometricsEnabled
            } else {
                mode.descriptionWhenBiometricsNotEnabled
            }
            ContentHeader(
                modifier = Modifier.fillMaxWidth(),
                config = ContentHeaderConfig(description = description)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_SMALL.dp)
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SPACING_SMALL.dp),
                    text = mode.textAbovePin,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                PinFieldLayout(
                    modifier = Modifier.fillMaxWidth(),
                    state = state,
                    onPinInput = { quickPin ->
                        onEventSent(Event.OnQuickPinEntered(quickPin))
                    }
                )
            }
        }

        is BiometricMode.Login -> {
            AppIconAndText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SPACING_LARGE.dp),
                appIconAndTextData = AppIconAndTextDataUi(),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_LARGE.dp),
                verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp, Alignment.Top)
            ) {
                Text(
                    text = mode.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                val subtitle = if (state.userBiometricsAreEnabled) {
                    mode.subTitleWhenBiometricsEnabled
                } else {
                    mode.subTitleWhenBiometricsNotEnabled
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            PinFieldLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SPACING_LARGE.dp),
                state = state,
                onPinInput = { quickPin ->
                    onEventSent(Event.OnQuickPinEntered(quickPin))
                }
            )
        }
    }
}

@Composable
private fun PinFieldLayout(
    modifier: Modifier = Modifier,
    state: State,
    onPinInput: (String) -> Unit,
) {
    WrapPinTextField(
        modifier = modifier,
        onPinUpdate = onPinInput,
        length = state.quickPinSize,
        hasError = !state.quickPinError.isNullOrEmpty(),
        errorMessage = state.quickPinError,
        visualTransformation = PasswordVisualTransformation(),
        pinWidth = 42.dp,
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
                    mode = BiometricMode.Default(
                        descriptionWhenBiometricsEnabled = stringResource(R.string.loading_biometry_biometrics_enabled_description),
                        descriptionWhenBiometricsNotEnabled = stringResource(R.string.loading_biometry_biometrics_not_enabled_description),
                        textAbovePin = stringResource(R.string.biometric_default_mode_text_above_pin_field),
                    ),
                    isPreAuthorization = true,
                    onSuccessNavigation = ConfigNavigation(
                        navigationType = NavigationType.PushScreen(CommonScreens.Biometric)
                    ),
                    onBackNavigationConfig = OnBackNavigationConfig(
                        onBackNavigation = ConfigNavigation(
                            navigationType = NavigationType.PushScreen(CommonScreens.Biometric),
                        ),
                        hasToolbarBackIcon = true
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