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

package eu.europa.ec.commonfeature.ui.success

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.config.SuccessUIConfig
import eu.europa.ec.resourceslogic.theme.values.colorSuccess
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.WrapPrimaryButton
import eu.europa.ec.uilogic.component.wrap.WrapSecondaryButton
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.StartupScreens
import eu.europa.ec.uilogic.navigation.helper.generateNewTaskDeepLink
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun SuccessScreen(
    navController: NavController,
    viewModel: SuccessViewModel
) {
    val context = LocalContext.current

    ContentScreen(
        onBack = { viewModel.setEvent(Event.BackPressed) },
        navigatableAction = ScreenNavigateAction.NONE
    ) { paddingValues ->
        SuccessScreenView(
            state = viewModel.viewState.value,
            effectFlow = viewModel.effect,
            onEventSent = { event -> viewModel.setEvent(event) },
            onNavigationRequested = { navigationEffect ->
                when (navigationEffect) {
                    is Effect.Navigation.SwitchScreen -> {
                        navController.navigate(navigationEffect.screenRoute) {
                            popUpTo(CommonScreens.Success.screenRoute) {
                                inclusive = true
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
                        navController.handleDeepLink(
                            generateNewTaskDeepLink(
                                context,
                                navigationEffect.screen,
                                navigationEffect.arguments,
                                navigationEffect.flags
                            )
                        )
                    }
                }
            },
            paddingValues = paddingValues
        )
    }
}

@Composable
private fun SuccessScreenView(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSent: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ContentTitle(
                title = state.successConfig.header,
                titleStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.colorSuccess
                ),
                subtitle = state.successConfig.content,
            )
            VSpacer.Small()
        }

        val imageConfig = state.successConfig.imageConfig
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when {
                // DEFAULT
                imageConfig.type == SuccessUIConfig.ImageConfig.Type.DEFAULT -> {
                    Image(
                        modifier = Modifier.fillMaxWidth(0.6f),
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = imageConfig.contentDescription,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.colorSuccess),
                        contentScale = ContentScale.FillWidth
                    )
                }
                // Image
                imageConfig.type == SuccessUIConfig.ImageConfig.Type.DRAWABLE && imageConfig.drawableRes != null -> {
                    Image(
                        painter = painterResource(id = imageConfig.drawableRes),
                        contentDescription = imageConfig.contentDescription
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.successConfig.buttonConfig.forEach { buttonConfig ->
                Button(
                    onEventSent = onEventSent,
                    config = buttonConfig
                )
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

@Composable
private fun Button(
    onEventSent: (Event) -> Unit,
    config: SuccessUIConfig.ButtonConfig
) {
    when (config.style) {
        SuccessUIConfig.ButtonConfig.Style.PRIMARY -> {
            WrapPrimaryButton(
                onClick = { onEventSent(Event.ButtonClicked(config)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                ButtonRow(text = config.text)
            }
        }

        SuccessUIConfig.ButtonConfig.Style.OUTLINE -> {
            WrapSecondaryButton(
                onClick = { onEventSent(Event.ButtonClicked(config)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                ButtonRow(text = config.text)
            }
        }
    }
}

@Composable
private fun ButtonRow(text: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview
@Composable
fun SuccessPreview() {
    SuccessScreenView(
        state = State(
            successConfig = SuccessUIConfig(
                header = "Success",
                content = "",
                imageConfig = SuccessUIConfig.ImageConfig(
                    type = SuccessUIConfig.ImageConfig.Type.DEFAULT
                ),
                buttonConfig = listOf(
                    SuccessUIConfig.ButtonConfig(
                        text = "back",
                        style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                        navigation = ConfigNavigation(
                            navigationType = NavigationType.POP,
                            screenToNavigate = StartupScreens.Splash
                        )
                    )
                ),
                onBackScreenToNavigate = ConfigNavigation(
                    navigationType = NavigationType.POP,
                    screenToNavigate = StartupScreens.Splash
                ),
            )
        ),
        effectFlow = Channel<Effect>().receiveAsFlow(),
        onEventSent = {},
        onNavigationRequested = {},
        paddingValues = PaddingValues(16.dp)
    )
}