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

package eu.europa.ec.proximityfeature.ui.qr

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import eu.europa.ec.proximityfeature.ui.qr.component.rememberQrBitmapPainter
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.backgroundDefault
import eu.europa.ec.resourceslogic.theme.values.textSecondaryDark
import eu.europa.ec.resourceslogic.theme.values.topCorneredShapeSmall
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapImage
import eu.europa.ec.uilogic.navigation.ProximityScreens
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun ProximityQRScreen(
    navController: NavController,
    viewModel: ProximityQRViewModel
) {
    val state = viewModel.viewState.value
    val context = LocalContext.current

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = ScreenNavigateAction.CANCELABLE,
        onBack = { viewModel.setEvent(Event.GoBack) },
        contentErrorConfig = state.error,
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onNavigationRequested = { navigationEffect ->
                when (navigationEffect) {
                    is Effect.Navigation.SwitchScreen -> {
                        navController.navigate(navigationEffect.screenRoute) {
                            popUpTo(ProximityScreens.QR.screenRoute) {
                                inclusive = true
                            }
                        }
                    }

                    is Effect.Navigation.Pop -> {
                        navController.popBackStack()
                    }
                }
            },
            paddingValues = paddingValues
        )
    }

    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.Init)
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME
    ) {
        viewModel.setEvent(
            Event.NfcEngagement(
                context as ComponentActivity,
                true
            )
        )
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_PAUSE
    ) {
        viewModel.setEvent(
            Event.NfcEngagement(
                context as ComponentActivity,
                false
            )
        )
    }
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
) {
    val configuration = LocalConfiguration.current
    val qrSize = (configuration.screenWidthDp / 1.5).dp

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(paddingValues)
        ) {
            ContentTitle(
                title = stringResource(id = R.string.proximity_qr_title),
                subtitle = stringResource(id = R.string.proximity_qr_subtitle)
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                QRCode(
                    qrCode = state.qrCode,
                    qrSize = qrSize
                )
            }
        }

        NFCSection()
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
private fun NFCSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                color = MaterialTheme.colorScheme.backgroundDefault,
                shape = MaterialTheme.shapes.topCorneredShapeSmall
            )
            .padding(vertical = SPACING_EXTRA_LARGE.dp, horizontal = SPACING_LARGE.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.proximity_qr_use_nfc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textSecondaryDark
        )
        VSpacer.Medium()
        WrapIcon(
            iconData = AppIcons.NFC,
            modifier = Modifier.size(96.dp),
            customTint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun QRCode(
    modifier: Modifier = Modifier,
    qrCode: String,
    qrSize: Dp
) {

    val primaryPixelsColor = if (isSystemInDarkTheme()) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.primary
    }

    val secondaryPixelsColor = if (isSystemInDarkTheme()) {
        Color.White
    } else {
        MaterialTheme.colorScheme.background
    }

    if (qrCode.isNotEmpty()) {
        WrapImage(
            modifier = modifier,
            painter = rememberQrBitmapPainter(
                content = qrCode,
                primaryPixelsColor = primaryPixelsColor.toArgb(),
                secondaryPixelsColor = secondaryPixelsColor.toArgb(),
                size = qrSize
            ),
            contentDescription = stringResource(id = R.string.content_description_qr_code)
        )
    }
}

@ThemeModePreviews
@Composable
private fun ContentPreview() {
    PreviewTheme {
        Content(
            state = State(
                isLoading = false,
                error = null,
                qrCode = "some qr code"
            ),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onNavigationRequested = {},
            paddingValues = PaddingValues(SPACING_MEDIUM.dp)
        )
    }
}