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

package eu.europa.ec.loginfeature.ui.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.ThemeManager
import eu.europa.ec.resourceslogic.theme.templates.ThemeDimensTemplate
import eu.europa.ec.resourceslogic.theme.values.ThemeColors
import eu.europa.ec.resourceslogic.theme.values.ThemeShapes
import eu.europa.ec.resourceslogic.theme.values.ThemeTypography
import eu.europa.ec.resourceslogic.theme.values.bottomCorneredShapeSmall
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.wrap.WrapPrimaryButton
import eu.europa.ec.uilogic.component.wrap.WrapSecondaryButton
import eu.europa.ec.uilogic.navigation.LoginScreens
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun WelcomeScreen(
    navController: NavController,
    viewModel: WelcomeScreenViewModel
) {
    Content(
        effectFlow = viewModel.effect,
        onEventSend = { viewModel.setEvent(it) },
        onNavigationRequested = { navigationEffect ->
            when (navigationEffect) {
                is Effect.Navigation.Login -> {
                    // TODO navigate to Login screen
                }

                is Effect.Navigation.Faq -> {
                    navController.navigate(navigationEffect.screen) {
                        popUpTo(LoginScreens.Faq.screenRoute) { inclusive = true }
                    }
                }
            }
        }
    )
}

@Composable
fun Content(
    effectFlow: Flow<Effect>?,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.bottomCorneredShapeSmall
                ),
            contentAlignment = Alignment.Center
        ) {
            val image = AppIcons.Logo
            Image(
                painter = painterResource(
                    id = image.resourceId!!
                ),
                contentDescription = stringResource(id = image.contentDescriptionId)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            WrapPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = { onEventSend(Event.GoToLogin) }
            ) {
                Text(
                    text = stringResource(id = R.string.label_login).uppercase(),
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            WrapSecondaryButton(
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = { onEventSend(Event.GoToFaq) }
            ) {
                Text(
                    text = stringResource(id = R.string.label_read_faq),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        effectFlow?.onEach { effect ->
            when (effect) {
                is Effect.Navigation.Login -> onNavigationRequested(effect)
                is Effect.Navigation.Faq -> onNavigationRequested(effect)
            }
        }?.collect()
    }
}

@Composable
@Preview(showSystemUi = true, showBackground = true)
fun WelcomeScreenPreview() {
    ThemeManager.Builder()
        .withLightColors(ThemeColors.lightColors)
        .withDarkColors(ThemeColors.darkColors)
        .withTypography(ThemeTypography.typo)
        .withShapes(ThemeShapes.shapes)
        .withDimensions(
            ThemeDimensTemplate(
                screenPadding = 10.0
            )
        )
        .build()
    ThemeManager.instance.Theme {
        Content(
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {}
        )
    }
}
