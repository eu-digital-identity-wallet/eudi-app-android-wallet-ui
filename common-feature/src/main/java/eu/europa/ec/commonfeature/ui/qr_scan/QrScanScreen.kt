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

package eu.europa.ec.commonfeature.ui.qr_scan

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import eu.europa.ec.commonfeature.ui.qr_scan.component.QrCodeAnalyzer
import eu.europa.ec.commonfeature.ui.qr_scan.component.qrBorderCanvas
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ErrorInfo
import eu.europa.ec.uilogic.component.PreMeasuredContentWithAnimatedVisibility
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SIZE_LARGE
import eu.europa.ec.uilogic.component.utils.SIZE_MEDIUM
import eu.europa.ec.uilogic.component.utils.SIZE_XX_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.WrapCard
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.extension.openAppSettings
import eu.europa.ec.uilogic.extension.throttledClickable
import eu.europa.ec.uilogic.navigation.CommonScreens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun QrScanScreen(
    navController: NavController,
    viewModel: QrScanViewModel
) {
    val state = viewModel.viewState.value
    val context = LocalContext.current

    ContentScreen(
        isLoading = false,
        navigatableAction = ScreenNavigateAction.CANCELABLE,
        onBack = { viewModel.setEvent(Event.GoBack) },
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(context, navigationEffect, navController)
            },
            paddingValues = paddingValues,
        )
    }
}

private fun handleNavigationEffect(
    context: Context,
    navigationEffect: Effect.Navigation,
    navController: NavController
) {
    when (navigationEffect) {
        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute) {
                popUpTo(CommonScreens.QrScan.screenRoute) {
                    inclusive = true
                }
            }
        }

        is Effect.Navigation.Pop -> {
            navController.popBackStack()
        }

        is Effect.Navigation.GoToAppSettings -> context.openAppSettings()
    }
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        ContentTitle(
            title = state.qrScannedConfig.title,
            subtitle = state.qrScannedConfig.subTitle
        )

        // Occupy the rest of the screen.
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OpenCamera(
                hasCameraPermission = state.hasCameraPermission,
                shouldShowPermissionRational = state.shouldShowPermissionRational,
                onEventSend = onEventSend,
                onQrScanned = { qrCode ->
                    onEventSend(Event.OnQrScanned(resultQr = qrCode))
                }
            )

            VSpacer.Large()

            PreMeasuredContentWithAnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                showContent = state.showInformativeText
            ) {
                InformativeText(text = state.informativeText)
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun OpenCamera(
    hasCameraPermission: Boolean,
    shouldShowPermissionRational: Boolean,
    onEventSend: (Event) -> Unit,
    onQrScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp
    val cameraSize = (screenWidth - (SPACING_LARGE * 2)).dp
    val cameraShape = RoundedCornerShape(SIZE_LARGE.dp)

    val permissionState = rememberPermissionState(permission = android.Manifest.permission.CAMERA)
    when {
        permissionState.status.isGranted -> onEventSend(Event.CameraAccessGranted)
        permissionState.status.shouldShowRationale -> onEventSend(Event.ShowPermissionRational)

        else -> {
            LaunchedEffect(Unit) {
                permissionState.launchPermissionRequest()
            }
        }
    }

    // The space the Camera is going to occupy.
    Box(
        modifier = Modifier
            .size(cameraSize)
            .background(
                color = Color.Black,
                shape = cameraShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission) {

            // The Camera.
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(cameraShape),
                factory = { context ->

                    val previewView = PreviewView(context)
                    val preview = Preview.Builder().build()

                    val selector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()

                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(context),
                        QrCodeAnalyzer { result ->
                            onQrScanned(result)
                        }
                    )
                    try {
                        cameraProviderFuture.get().bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    previewView
                }
            )
        } else if (shouldShowPermissionRational) {
            ErrorInfo(
                modifier = Modifier.throttledClickable { onEventSend(Event.GoToAppSettings) },
                informativeText = stringResource(id = R.string.qr_scan_permission_not_granted),
                contentColor = Color.White,
                iconAlpha = 1f
            )
        }

        // Draw indicators.
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val borderColor = MaterialTheme.colorScheme.primary
            Canvas(
                modifier = Modifier.matchParentSize()
            ) {
                qrBorderCanvas(
                    borderColor = borderColor,
                    curve = SIZE_MEDIUM.dp,
                    strokeWidth = SIZE_MEDIUM.dp,
                    capSize = SIZE_XX_LARGE.dp,
                    gapAngle = SIZE_EXTRA_SMALL,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun InformativeText(text: String) {
    WrapCard {
        Row(
            modifier = Modifier.padding(all = SPACING_SMALL.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WrapIcon(AppIcons.Error)
            Text(
                modifier = Modifier.padding(all = SPACING_SMALL.dp),
                text = text,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun InformativeTextPreview() {
    PreviewTheme {
        InformativeText(
            text = stringResource(R.string.qr_scan_informative_text_presentation_flow)
        )
    }
}