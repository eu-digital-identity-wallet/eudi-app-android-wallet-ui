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

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.utils.PreviewTheme
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.bottomCorneredShapeSmall
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.WrapImage
import eu.europa.ec.uilogic.component.wrap.WrapPrimaryButton
import eu.europa.ec.uilogic.component.wrap.WrapSecondaryButton
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
        state = viewModel.viewState.value,
        effectFlow = viewModel.effect,
        onEventSend = { viewModel.setEvent(it) },
        onNavigationRequested = { navigationEffect ->
            when (navigationEffect) {
                is Effect.Navigation.SwitchScreen -> navController.navigate(navigationEffect.screenRoute)
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
    var showContent by remember { mutableStateOf(state.showContent) }
    val animatedWeight by animateFloatAsState(
        targetValue = if (showContent) 0.75f else 1f,
        animationSpec = tween(
            delayMillis = state.enterAnimationDelay,
            durationMillis = state.enterAnimationDuration,
            easing = LinearOutSlowInEasing
        ),
        label = ""
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedWeight)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.bottomCorneredShapeSmall
                ),
            contentAlignment = Alignment.Center
        ) {
            WrapImage(iconData = AppIcons.Logo)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = SPACING_LARGE.dp),
            verticalArrangement = Arrangement.Center
        ) {

            WrapPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = { onEventSend(Event.NavigateToLogin) }
            ) {
                Text(
                    text = stringResource(id = R.string.welcome_login_button_title),
                    fontWeight = FontWeight.Medium,
                )
            }

            VSpacer.Medium()

            WrapSecondaryButton(
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = { onEventSend(Event.NavigateToFaq) }
            ) {
                Text(
                    text = stringResource(id = R.string.welcome_faq_button_title),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        showContent = true
        effectFlow?.onEach { effect ->
            when (effect) {
                is Effect.Navigation.SwitchScreen -> onNavigationRequested(effect)
            }
        }?.collect()
    }
}

@Composable
@Preview(showSystemUi = true, showBackground = true)
private fun WelcomeScreenPreview() {
    PreviewTheme {
        Content(
            state = State(),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {}
        )
    }
}
