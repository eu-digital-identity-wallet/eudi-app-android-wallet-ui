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

package eu.europa.ec.dashboardfeature.ui

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.model.DocumentStatusUi
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.bottomCorneredShapeSmall
import eu.europa.ec.resourceslogic.theme.values.success
import eu.europa.ec.resourceslogic.theme.values.textPrimaryDark
import eu.europa.ec.resourceslogic.theme.values.textSecondaryLight
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.IconData
import eu.europa.ec.uilogic.component.ScalableText
import eu.europa.ec.uilogic.component.content.ContentGradient
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.GradientEdge
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.FabData
import eu.europa.ec.uilogic.component.wrap.WrapCard
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapImage
import eu.europa.ec.uilogic.component.wrap.WrapPrimaryExtendedFab
import eu.europa.ec.uilogic.component.wrap.WrapSecondaryExtendedFab
import eu.europa.ec.uilogic.extension.getPendingDeepLink
import eu.europa.ec.uilogic.navigation.helper.handleDeepLinkAction
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel
) {
    val state = viewModel.viewState.value
    val context = LocalContext.current

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { viewModel.setEvent(Event.Pop) },
        contentErrorConfig = state.error
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(navigationEffect, navController, context)
            },
            paddingValues = paddingValues
        )
    }

    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.Init)
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController,
    context: Context
) {
    when (navigationEffect) {
        is Effect.Navigation.Pop -> navController.popBackStack()
        is Effect.Navigation.SwitchScreen -> navController.navigate(navigationEffect.screenRoute)
        is Effect.Navigation.OpenDeepLinkAction -> context.getPendingDeepLink()?.let {
            handleDeepLinkAction(navController, it)
        }
    }
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Title section.
        Title(
            message = stringResource(id = R.string.dashboard_title),
            userName = state.userName,
            image = AppIcons.User,
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

        FabContent(
            paddingValues = paddingValues,
            onEventSend = onEventSend
        )
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
private fun FabContent(
    modifier: Modifier = Modifier,
    onEventSend: (Event) -> Unit,
    paddingValues: PaddingValues
) {
    val titleSmallTextStyle = MaterialTheme.typography.titleSmall
        .copy(color = MaterialTheme.colorScheme.textPrimaryDark)

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
                modifier = Modifier.size(24.dp),
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
    userName: String,
    image: IconData,
    paddingValues: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary,
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            WrapImage(
                iconData = image,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(SIZE_SMALL.dp))
            )
            Column(
                modifier = Modifier
                    .padding(start = SPACING_MEDIUM.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textSecondaryLight
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.background
                )
            }
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

        items(documents.size) { index ->
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
    WrapCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp),
        onClick = { onEventSend(Event.NavigateToDocument(documentId = dataItem.documentId)) },
        throttleClicks = true,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            WrapIcon(
                iconData = AppIcons.Id,
                customTint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = dataItem.documentType.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.textPrimaryDark
            )
            Text(
                text = dataItem.documentStatus.title,
                style = MaterialTheme.typography.bodyMedium,
                color = when (dataItem.documentStatus) {
                    DocumentStatusUi.ACTIVE -> MaterialTheme.colorScheme.success
                    DocumentStatusUi.INACTIVE -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun DashboardScreenPreview() {
    PreviewTheme {
        val documents = listOf(
            DocumentUi(
                documentId = "0",
                documentType = DocumentTypeUi.DIGITAL_ID,
                documentStatus = DocumentStatusUi.ACTIVE,
                documentImage = "image1"
            ),
            DocumentUi(
                documentId = "1",
                documentType = DocumentTypeUi.DRIVING_LICENCE,
                documentStatus = DocumentStatusUi.ACTIVE,
                documentImage = "image2"
            ),
            DocumentUi(
                documentId = "2",
                documentType = DocumentTypeUi.OTHER,
                documentStatus = DocumentStatusUi.ACTIVE,
                documentImage = "image3"
            )
        )
        Content(
            state = State(
                isLoading = false,
                error = null,
                userName = "Jane",
                documents = documents + documents
            ),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(SPACING_MEDIUM.dp)
        )
    }
}