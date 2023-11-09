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

package eu.europa.ec.dashboardfeature.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.model.DocumentItemUi
import eu.europa.ec.commonfeature.model.DocumentStatusUi
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.resourceslogic.theme.values.backgroundPaper
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.HeaderData
import eu.europa.ec.uilogic.component.HeaderLarge
import eu.europa.ec.uilogic.component.InfoTextWithNameAndValueData
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.details.DetailsContent
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun DocumentDetailsScreen(
    navController: NavController,
    viewModel: DocumentDetailsViewModel
) {
    val state = viewModel.viewState.value

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { viewModel.setEvent(Event.Pop) }
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(navigationEffect, navController)
            },
            paddingValues
        )
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME
    ) {
        viewModel.setEvent(Event.Init)
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController
) {
    when (navigationEffect) {
        is Effect.Navigation.Pop -> navController.popBackStack()
    }
}

@Composable
private fun DocumentDetailsTopBar(
    onEventSend: (Event) -> Unit
) {
    Box(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.primary)
            .fillMaxWidth()
            .padding(
                start = SPACING_LARGE.dp,
                end = SPACING_LARGE.dp,
                top = SPACING_EXTRA_LARGE.dp
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        WrapIcon(
            modifier = Modifier.clickable { onEventSend(Event.Pop) },
            iconData = AppIcons.Close,
            customTint = MaterialTheme.colorScheme.backgroundPaper
        )
    }
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues
) {

    if (state.document != null) {
        val headerData = HeaderData(
            title = state.document.documentType.title,
            subtitle = state.userName ?: "",
            AppIcons.User,
            AppIcons.IdStroke
        )
        val detailsData = state.document.documentItems.map {
            InfoTextWithNameAndValueData(
                infoName = it.title,
                infoValue = it.value
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            DocumentDetailsTopBar(onEventSend = onEventSend)
            HeaderLarge(data = headerData)
            DetailsContent(
                modifier = Modifier
                    .padding(
                        start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                        end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                    ),
                data = detailsData
            )
        }
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation.Pop -> onNavigationRequested(effect)
            }
        }.collect()
    }
}

@ThemeModePreviews
@Composable
private fun ContentPreview() {
    PreviewTheme {

        val state = State(
            document = DocumentUi(
                documentId = 2,
                documentType = DocumentTypeUi.DIGITAL_ID,
                documentStatus = DocumentStatusUi.ACTIVE,
                documentImage = "image3",
                documentItems = (1..10).map {
                    DocumentItemUi("Name $it", "Value $it")
                }
            ),
            userName = "Jane Doe"
        )

        Content(
            state = state,
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onNavigationRequested = {},
            onEventSend = {},
            paddingValues = PaddingValues(32.dp)
        )
    }
}

@ThemeModePreviews
@Composable
private fun TopBarPreview() {
    PreviewTheme {
        DocumentDetailsTopBar(
            onEventSend = {}
        )
    }
}