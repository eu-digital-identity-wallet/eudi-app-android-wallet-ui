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

package eu.europa.ec.dashboardfeature.ui.dashboard

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.model.DocumentUiIssuanceState
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.allCorneredShapeSmall
import eu.europa.ec.resourceslogic.theme.values.backgroundDefault
import eu.europa.ec.resourceslogic.theme.values.bottomCorneredShapeSmall
import eu.europa.ec.resourceslogic.theme.values.textDisabledDark
import eu.europa.ec.resourceslogic.theme.values.textPrimaryDark
import eu.europa.ec.resourceslogic.theme.values.textSecondaryDark
import eu.europa.ec.resourceslogic.theme.values.warning
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ModalOptionUi
import eu.europa.ec.uilogic.component.ScalableText
import eu.europa.ec.uilogic.component.UserImageOrPlaceholder
import eu.europa.ec.uilogic.component.content.ContentGradient
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.GradientEdge
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.ALPHA_DISABLED
import eu.europa.ec.uilogic.component.utils.ALPHA_ENABLED
import eu.europa.ec.uilogic.component.utils.HSpacer
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.SIZE_LARGE
import eu.europa.ec.uilogic.component.utils.SIZE_MEDIUM
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.BottomSheetWithOptionsList
import eu.europa.ec.uilogic.component.wrap.DialogBottomSheet
import eu.europa.ec.uilogic.component.wrap.FabData
import eu.europa.ec.uilogic.component.wrap.GenericBaseSheetContent
import eu.europa.ec.uilogic.component.wrap.WrapCard
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapIconButton
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapPrimaryExtendedFab
import eu.europa.ec.uilogic.component.wrap.WrapSecondaryExtendedFab
import eu.europa.ec.uilogic.extension.IconWarningIndicator
import eu.europa.ec.uilogic.extension.dashedBorder
import eu.europa.ec.uilogic.extension.finish
import eu.europa.ec.uilogic.extension.getPendingDeepLink
import eu.europa.ec.uilogic.extension.openAppSettings
import eu.europa.ec.uilogic.extension.openBleSettings
import eu.europa.ec.uilogic.extension.openIntentChooser
import eu.europa.ec.uilogic.extension.throttledClickable
import eu.europa.ec.uilogic.navigation.helper.handleDeepLinkAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel
) {
    val state = viewModel.viewState.value
    val context = LocalContext.current

    val isBottomSheetOpen = state.isBottomSheetOpen
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { viewModel.setEvent(Event.Pop) },
        contentErrorConfig = state.error
    ) { paddingValues ->
        Content(
            context = context,
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(navigationEffect, navController, context)
            },
            paddingValues = paddingValues,
            coroutineScope = scope,
            modalBottomSheetState = bottomSheetState
        )

        if (isBottomSheetOpen) {
            WrapModalBottomSheet(
                onDismissRequest = {
                    viewModel.setEvent(
                        Event.BottomSheet.UpdateBottomSheetState(
                            isOpen = false
                        )
                    )
                },
                sheetState = bottomSheetState
            ) {
                DashboardSheetContent(
                    sheetContent = state.sheetContent,
                    state = state,
                    onEventSent = {
                        viewModel.setEvent(it)
                    }
                )
            }
        }
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME
    ) {
        viewModel.setEvent(
            Event.Init(
                deepLinkUri = context.getPendingDeepLink()
            )
        )
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_PAUSE
    ) {
        viewModel.setEvent(
            Event.OnPause
        )
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController,
    context: Context
) {
    when (navigationEffect) {
        is Effect.Navigation.Pop -> context.finish()
        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute) {
                popUpTo(navigationEffect.popUpToScreenRoute) {
                    inclusive = navigationEffect.inclusive
                }
            }
        }

        is Effect.Navigation.OpenDeepLinkAction -> {
            handleDeepLinkAction(
                navController,
                navigationEffect.deepLinkUri,
                navigationEffect.arguments
            )
        }

        is Effect.Navigation.OnAppSettings -> context.openAppSettings()
        is Effect.Navigation.OnSystemSettings -> context.openBleSettings()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    context: Context,
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
    coroutineScope: CoroutineScope,
    modalBottomSheetState: SheetState,
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Title section.
        Title(
            message = stringResource(id = R.string.dashboard_title),
            userFirstName = state.userFirstName,
            userBase64Image = state.userBase64Image,
            allowUserInteraction = state.allowUserInteraction,
            onEventSend = onEventSend,
            paddingValues = paddingValues
        )

        // Documents section.
        ContentGradient(
            modifier = Modifier.weight(1f),
            gradientEdge = GradientEdge.BOTTOM
        ) {
            DocumentsList(
                documents = state.documents,
                modifier = Modifier,
                onEventSend = onEventSend,
                paddingValues = paddingValues
            )
        }

        if (state.allowUserInteraction) {
            FabContent(
                paddingValues = paddingValues,
                onEventSend = onEventSend
            )
        }
    }

    if (state.bleAvailability == BleAvailability.NO_PERMISSION) {
        RequiredPermissionsAsk(state, onEventSend)
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)

                is Effect.CloseBottomSheet -> {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                    }.invokeOnCompletion {
                        if (!modalBottomSheetState.isVisible) {
                            onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                        }
                    }
                }

                is Effect.ShowBottomSheet -> {
                    onEventSend(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
                }

                is Effect.DocumentsFetched -> {
                    onEventSend(Event.TryIssuingDeferredDocuments(effect.deferredDocs))
                }

                is Effect.ShareLogFile -> {
                    context.openIntentChooser(
                        effect.intent,
                        effect.chooserTitle
                    )
                }
            }
        }.collect()
    }
}

@Composable
private fun DashboardSheetContent(
    sheetContent: DashboardBottomSheetContent,
    state: State,
    onEventSent: (event: Event) -> Unit
) {
    when (sheetContent) {
        is DashboardBottomSheetContent.Options -> {
            GenericBaseSheetContent(
                titleContent = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(id = R.string.dashboard_bottom_sheet_options_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.textPrimaryDark
                        )
                        WrapIconButton(
                            iconData = AppIcons.Close,
                            customTint = MaterialTheme.colorScheme.primary,
                            onClick = { onEventSent(Event.BottomSheet.Close) }
                        )
                    }
                },
                bodyContent = {

                    for (option in sheetContent.options) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.allCorneredShapeSmall)
                                .throttledClickable(
                                    onClick = { onEventSent(option.event) }
                                )
                                .padding(
                                    vertical = SPACING_SMALL.dp,
                                    horizontal = SPACING_EXTRA_SMALL.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            WrapIcon(
                                iconData = option.icon,
                                customTint = MaterialTheme.colorScheme.primary
                            )
                            HSpacer.Medium()
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.textPrimaryDark
                            )
                        }

                        VSpacer.Medium()
                    }

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = state.appVersion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.textSecondaryDark,
                        textAlign = TextAlign.Center
                    )
                }
            )
        }

        is DashboardBottomSheetContent.Bluetooth -> {
            DialogBottomSheet(
                title = stringResource(id = R.string.dashboard_bottom_sheet_bluetooth_title),
                message = stringResource(id = R.string.dashboard_bottom_sheet_bluetooth_subtitle),
                positiveButtonText = stringResource(id = R.string.dashboard_bottom_sheet_bluetooth_primary_button_text),
                negativeButtonText = stringResource(id = R.string.dashboard_bottom_sheet_bluetooth_secondary_button_text),
                onPositiveClick = {
                    onEventSent(
                        Event.BottomSheet.Bluetooth.PrimaryButtonPressed(
                            sheetContent.availability
                        )
                    )
                },
                onNegativeClick = { onEventSent(Event.BottomSheet.Bluetooth.SecondaryButtonPressed) }
            )
        }

        is DashboardBottomSheetContent.DeferredDocumentPressed -> {
            DialogBottomSheet(
                title = stringResource(
                    id = R.string.dashboard_bottom_sheet_deferred_document_pressed_title,
                    sheetContent.documentUi.documentName
                ),
                message = stringResource(
                    id = R.string.dashboard_bottom_sheet_deferred_document_pressed_subtitle,
                    sheetContent.documentUi.documentName
                ),
                positiveButtonText = stringResource(id = R.string.dashboard_bottom_sheet_deferred_document_pressed_primary_button_text),
                negativeButtonText = stringResource(id = R.string.dashboard_bottom_sheet_deferred_document_pressed_secondary_button_text),
                onPositiveClick = {
                    onEventSent(
                        Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.PrimaryButtonPressed(
                            documentUi = sheetContent.documentUi
                        )
                    )
                },
                onNegativeClick = {
                    onEventSent(
                        Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.SecondaryButtonPressed(
                            documentUi = sheetContent.documentUi
                        )
                    )
                }
            )
        }

        is DashboardBottomSheetContent.DeferredDocumentsReady -> {
            BottomSheetWithOptionsList(
                title = stringResource(
                    id = R.string.dashboard_bottom_sheet_deferred_documents_ready_title
                ),
                message = stringResource(
                    id = R.string.dashboard_bottom_sheet_deferred_documents_ready_subtitle
                ),
                options = sheetContent.options,
                onEventSent = onEventSent,
            )
        }
    }
}

@Composable
private fun FabContent(
    modifier: Modifier = Modifier,
    onEventSend: (Event) -> Unit,
    paddingValues: PaddingValues
) {
    val titleSmallTextStyle = MaterialTheme.typography.titleSmall

    val secondaryFabData = FabData(
        text = {
            ScalableText(
                text = stringResource(id = R.string.dashboard_secondary_fab_text),
                textStyle = titleSmallTextStyle
            )
        },
        icon = { WrapIcon(iconData = AppIcons.Add) },
        onClick = { onEventSend(Event.Fab.SecondaryFabPressed) }
    )

    val primaryFabData = FabData(
        text = {
            ScalableText(
                text = stringResource(id = R.string.dashboard_primary_fab_text),
                textStyle = titleSmallTextStyle
            )
        },
        icon = {
            WrapIcon(
                modifier = Modifier.size(SIZE_LARGE.dp),
                iconData = AppIcons.NFC
            )
        },
        onClick = { onEventSend(Event.Fab.PrimaryFabPressed) },
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                bottom = SPACING_MEDIUM.dp,
                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr)
            ),
        horizontalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WrapSecondaryExtendedFab(data = secondaryFabData, modifier = Modifier.weight(1f))
        WrapPrimaryExtendedFab(data = primaryFabData, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Title(
    message: String,
    userFirstName: String,
    userBase64Image: String,
    allowUserInteraction: Boolean,
    onEventSend: (Event) -> Unit,
    paddingValues: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.secondary,
                shape = MaterialTheme.shapes.bottomCorneredShapeSmall
            )
            .padding(
                PaddingValues(
                    top = SPACING_EXTRA_LARGE.dp,
                    bottom = SPACING_EXTRA_LARGE.dp,
                    start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                    end = paddingValues.calculateEndPadding(LayoutDirection.Ltr)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            UserImageOrPlaceholder(
                userBase64Image = userBase64Image,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(SIZE_SMALL.dp))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = SPACING_MEDIUM.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                Text(
                    text = userFirstName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Black
                )
            }

            WrapIconButton(
                iconData = AppIcons.VerticalMore,
                customTint = MaterialTheme.colorScheme.primary,
                enabled = allowUserInteraction,
                onClick = {
                    onEventSend(Event.OptionsPressed)
                }
            )
        }
    }
}

@Composable
private fun DocumentsList(
    documents: List<DocumentUi>,
    modifier: Modifier = Modifier,
    onEventSend: (Event) -> Unit,
    paddingValues: PaddingValues
) {
    LazyVerticalGrid(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
            ),
        columns = GridCells.Adaptive(minSize = 148.dp),
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
        horizontalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {

        // Top Spacing
        items(2) {
            VSpacer.Large()
        }

        items(
            count = documents.size,
            key = { documents[it].documentId }
        ) { index ->
            CardListItem(
                dataItem = documents[index],
                onEventSend = onEventSend
            )
        }

        // Bottom Spacing
        items(2) {
            VSpacer.Large()
        }
    }
}

@Composable
private fun CardListItem(
    dataItem: DocumentUi,
    onEventSend: (Event) -> Unit
) {
    val dottedLinesColor = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.textSecondaryDark
    } else {
        MaterialTheme.colorScheme.textDisabledDark
    }

    val borderModifier = when (dataItem.documentIssuanceState) {
        DocumentUiIssuanceState.Issued -> Modifier
        DocumentUiIssuanceState.Pending, DocumentUiIssuanceState.Failed -> Modifier
            .dashedBorder(
                color = dottedLinesColor,
                shape = RoundedCornerShape(SIZE_MEDIUM.dp),
                strokeWidth = 2.dp,
                gapLength = SIZE_SMALL.dp
            )
    }

    WrapCard(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .then(borderModifier),
        onClick = {
            when (dataItem.documentIssuanceState) {
                DocumentUiIssuanceState.Issued -> {
                    onEventSend(
                        Event.NavigateToDocument(
                            documentId = dataItem.documentId
                        )
                    )
                }

                DocumentUiIssuanceState.Pending, DocumentUiIssuanceState.Failed -> {
                    onEventSend(
                        Event.BottomSheet.DeferredDocument.DeferredNotReadyYet.DocumentSelected(
                            documentUi = dataItem
                        )
                    )
                }
            }
        },
        throttleClicks = true,
    ) {
        DocumentContent(dataItem)
    }
}

@Composable
private fun DocumentContent(dataItem: DocumentUi) {
    val documentState = dataItem.documentIssuanceState
    val iconData = AppIcons.Id
    val iconTint = when (documentState) {
        DocumentUiIssuanceState.Issued -> MaterialTheme.colorScheme.primary
        DocumentUiIssuanceState.Pending, DocumentUiIssuanceState.Failed -> MaterialTheme.colorScheme.textDisabledDark
    }
    val iconAlpha = when (documentState) {
        DocumentUiIssuanceState.Issued -> ALPHA_ENABLED
        DocumentUiIssuanceState.Pending, DocumentUiIssuanceState.Failed -> ALPHA_DISABLED
    }

    val warningIconData = when (documentState) {
        DocumentUiIssuanceState.Issued -> AppIcons.Warning
        DocumentUiIssuanceState.Pending -> AppIcons.ClockTimer
        DocumentUiIssuanceState.Failed -> AppIcons.ErrorFilled
    }
    val warningIconTint = when (documentState) {
        DocumentUiIssuanceState.Issued, DocumentUiIssuanceState.Pending -> {
            MaterialTheme.colorScheme.warning
        }

        DocumentUiIssuanceState.Failed -> {
            MaterialTheme.colorScheme.error
        }

    }
    val documentNameColor = when (documentState) {
        DocumentUiIssuanceState.Issued -> {
            MaterialTheme.colorScheme.textPrimaryDark
        }

        DocumentUiIssuanceState.Pending, DocumentUiIssuanceState.Failed -> {
            MaterialTheme.colorScheme.textDisabledDark
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SPACING_MEDIUM.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            WrapIcon(
                iconData = iconData,
                customTint = iconTint,
                contentAlpha = iconAlpha
            )
            if (documentState == DocumentUiIssuanceState.Issued) {
                if (dataItem.documentHasExpired) {
                    IconWarningIndicator(
                        backgroundColor = MaterialTheme.colorScheme.backgroundDefault
                    )
                }
            } else {
                IconWarningIndicator(
                    iconData = warningIconData,
                    customTint = warningIconTint,
                    backgroundColor = MaterialTheme.colorScheme.backgroundDefault
                )
            }
        }
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            ScalableText(
                text = dataItem.documentName,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    color = documentNameColor
                )
            )
        }
        VSpacer.Small()
        ExpirationInfo(dataItem)
    }
}

@Composable
private fun ExpirationInfo(
    document: DocumentUi,
) {
    val textStyle = MaterialTheme.typography.bodySmall
        .copy(color = MaterialTheme.colorScheme.textSecondaryDark)

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        with(document) {
            when (documentIssuanceState) {
                DocumentUiIssuanceState.Issued -> {
                    if (documentExpirationDateFormatted.isNotBlank()) {
                        if (documentHasExpired) {
                            val annotatedText = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        fontStyle = textStyle.fontStyle,
                                        color = MaterialTheme.colorScheme.warning
                                    )
                                ) {
                                    append(stringResource(id = R.string.dashboard_document_has_expired_one))
                                }

                                append(stringResource(id = R.string.dashboard_document_has_expired_two))
                            }
                            Text(text = annotatedText, style = textStyle)
                        } else {
                            Text(
                                text = stringResource(id = R.string.dashboard_document_has_not_expired),
                                style = textStyle
                            )
                        }

                        //Expiration Date
                        Text(text = documentExpirationDateFormatted, style = textStyle)
                    }
                }

                DocumentUiIssuanceState.Pending -> {
                    val annotatedText = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontStyle = textStyle.fontStyle,
                                color = MaterialTheme.colorScheme.warning
                            )
                        ) {
                            append(stringResource(id = R.string.dashboard_document_deferred_pending))
                        }
                    }
                    Text(text = annotatedText, style = textStyle)
                }

                DocumentUiIssuanceState.Failed -> {
                    val annotatedText = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontStyle = textStyle.fontStyle,
                                color = MaterialTheme.colorScheme.error
                            )
                        ) {
                            append(stringResource(id = R.string.dashboard_document_deferred_failed))
                        }
                    }
                    Text(text = annotatedText, style = textStyle)
                }

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemeModePreviews
@Composable
private fun DashboardScreenPreview() {
    PreviewTheme {

        val context = LocalContext.current

        val documents = listOf(
            DocumentUi(
                documentId = "0",
                documentName = "National ID",
                documentIdentifier = DocumentIdentifier.PID,
                documentExpirationDateFormatted = "30 Mar 2050",
                documentHasExpired = false,
                documentImage = "image0",
                documentDetails = emptyList(),
                documentIssuanceState = DocumentUiIssuanceState.Issued
            ),
            DocumentUi(
                documentId = "1",
                documentName = "National ID",
                documentIdentifier = DocumentIdentifier.PID,
                documentExpirationDateFormatted = "",
                documentHasExpired = false,
                documentImage = "image1",
                documentDetails = emptyList(),
                documentIssuanceState = DocumentUiIssuanceState.Issued
            ),
            DocumentUi(
                documentId = "2",
                documentName = "National ID",
                documentIdentifier = DocumentIdentifier.PID,
                documentExpirationDateFormatted = "",
                documentHasExpired = true,
                documentImage = "image2",
                documentDetails = emptyList(),
                documentIssuanceState = DocumentUiIssuanceState.Issued
            ),
            DocumentUi(
                documentId = "3",
                documentName = "Driving License",
                documentIdentifier = DocumentIdentifier.MDL,
                documentExpirationDateFormatted = "25 Dec 2050",
                documentHasExpired = false,
                documentImage = "image3",
                documentDetails = emptyList(),
                documentIssuanceState = DocumentUiIssuanceState.Pending
            ),
            DocumentUi(
                documentId = "4",
                documentName = "Other",
                documentIdentifier = DocumentIdentifier.OTHER(
                    nameSpace = "",
                    docType = ""
                ),
                documentExpirationDateFormatted = "01 Jun 2020",
                documentHasExpired = true,
                documentImage = "image4",
                documentDetails = emptyList(),
                documentIssuanceState = DocumentUiIssuanceState.Pending
            ),
            DocumentUi(
                documentId = "5",
                documentName = "National ID",
                documentIdentifier = DocumentIdentifier.PID,
                documentExpirationDateFormatted = "30 Mar 2050",
                documentHasExpired = false,
                documentImage = "image5",
                documentDetails = emptyList(),
                documentIssuanceState = DocumentUiIssuanceState.Failed
            ),
        )
        Content(
            context = context,
            state = State(
                allowUserInteraction = true,
                isLoading = false,
                error = null,
                userFirstName = "Jane",
                documents = documents,
                isBottomSheetOpen = true,
                sheetContent = DashboardBottomSheetContent.Options(
                    listOf(
                        ModalOptionUi(
                            title = stringResource(R.string.dashboard_bottom_sheet_options_action_1),
                            icon = AppIcons.Edit,
                            event = Event.BottomSheet.Options.OpenChangeQuickPin
                        ),
                        ModalOptionUi(
                            title = stringResource(R.string.dashboard_bottom_sheet_options_action_2),
                            icon = AppIcons.QrScanner,
                            event = Event.BottomSheet.Options.OpenScanQr
                        ),
                        ModalOptionUi(
                            title = stringResource(R.string.dashboard_bottom_sheet_options_action_3),
                            icon = AppIcons.OpenNew,
                            event = Event.BottomSheet.Options.RetrieveLogs
                        )
                    )
                )
            ),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(SPACING_LARGE.dp),
            coroutineScope = rememberCoroutineScope(),
            modalBottomSheetState = rememberModalBottomSheetState()
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequiredPermissionsAsk(
    state: State,
    onEventSend: (Event) -> Unit
) {

    val permissions: MutableList<String> = mutableListOf()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 && state.isBleCentralClientModeEnabled) {
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    val permissionsState = rememberMultiplePermissionsState(permissions = permissions)

    when {
        permissionsState.allPermissionsGranted -> onEventSend(Event.StartProximityFlow)
        !permissionsState.allPermissionsGranted && permissionsState.shouldShowRationale -> {
            onEventSend(Event.OnShowPermissionsRational)
        }

        else -> {
            onEventSend(Event.OnPermissionStateChanged(BleAvailability.UNKNOWN))
            LaunchedEffect(Unit) {
                permissionsState.launchMultiplePermissionRequest()
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun SheetContentPreview() {
    PreviewTheme {
        DashboardSheetContent(
            sheetContent = DashboardBottomSheetContent.Options(
                listOf(
                    ModalOptionUi(
                        title = "Change Quick Pin",
                        icon = AppIcons.Edit,
                        event = Event.BottomSheet.Options.OpenChangeQuickPin
                    )
                )
            ),
            state = State(),
            onEventSent = {}
        )
    }
}