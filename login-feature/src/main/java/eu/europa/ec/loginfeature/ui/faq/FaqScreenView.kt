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

package eu.europa.ec.loginfeature.ui.faq

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import eu.europa.ec.loginfeature.model.FaqUiModel
import eu.europa.ec.resourceslogic.theme.values.allCorneredShapeSmall
import eu.europa.ec.resourceslogic.theme.values.textSecondaryDark
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.WrapExpandableCard
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapTextField
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun FaqScreen(
    navController: NavController,
    viewModel: FaqScreenViewModel
) {
    ContentScreen(
        isLoading = false,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(Event.Pop) }
    ) { paddingValues ->
        Content(
            state = viewModel.viewState.value,
            effectFlow = viewModel.effect,
            onEventSend = { event -> viewModel.setEvent(event) },
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(navigationEffect, navController)
            },
            paddingValues = paddingValues
        )
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController
) {
    when (navigationEffect) {
        is Effect.Navigation.Pop -> navController.popBackStack()

        is Effect.Navigation.SwitchScreen -> navController.navigate(navigationEffect.screen)
    }
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>?,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {

        var text by remember { mutableStateOf("") }

        ContentTitle("FAQs")

        WrapTextField(
            value = text,
            onValueChange = {
                onEventSend(Event.Search(it))
                text = it
            },
            singleLine = true,
            label = { Text("Search") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.allCorneredShapeSmall)
        )

        if (state.noSearchResult) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No results",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Black
                )
            }
        }

        ExpandableListScreen(sections = state.presentableFaqItems)
    }

    LaunchedEffect(Unit) {
        effectFlow?.onEach { effect ->
            when (effect) {
                is Effect.Navigation.SwitchScreen -> onNavigationRequested(effect)
                is Effect.Navigation.Pop -> onNavigationRequested(effect)
            }
        }?.collect()
    }
}

@Composable
private fun ExpandableListScreen(
    sections: List<FaqUiModel>
) {
    var expandedItemIndex by remember { mutableIntStateOf(-1) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        item {
            VSpacer.Medium()
        }

        sections.forEachIndexed { index, dataItem ->
            val isExpanded = index == expandedItemIndex
            item(key = index) {
                ExpandableListItem(
                    dataItem = dataItem,
                    isExpanded = isExpanded,
                    onHeaderClicked = {
                        expandedItemIndex = if (isExpanded) -1 else index
                    }
                )
            }
        }

        item {
            VSpacer.Medium()
        }
    }
}

@Composable
private fun ExpandableListItem(
    dataItem: FaqUiModel,
    isExpanded: Boolean,
    onHeaderClicked: () -> Unit
) {
    WrapExpandableCard(
        cardTitleContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dataItem.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Black
                )
                WrapIcon(
                    iconData = if (isExpanded) AppIcons.KeyboardArrowUp else AppIcons.KeyboardArrowDown,
                    customTint = MaterialTheme.colorScheme.primary
                )
            }
        },
        cardContent = {
            Text(
                text = dataItem.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textSecondaryDark
            )
        },
        cardTitlePadding = PaddingValues(SPACING_LARGE.dp),
        cardContentPadding = PaddingValues(horizontal = 0.dp, vertical = SPACING_SMALL.dp),
        onCardClick = { onHeaderClicked() },
        throttleClicks = false,
        expandCard = isExpanded
    )
}

@ThemeModePreviews
@Composable
private fun FaqScreenPreview() {
    PreviewTheme {
        Content(
            state = State(
                presentableFaqItems = listOf(
                    FaqUiModel(
                        title = "Question A goes Here",
                        description = "Lorem ipsum dolor sit amet," +
                                " consectetur adipiscing elit,"
                    ),
                    FaqUiModel(
                        title = "Question B goes Here",
                        description = "Duis aute irure dolor in reprehenderit in" +
                                " voluptate velit esse cillum dolore eu fugiat nulla pariatur."
                    ),
                    FaqUiModel(
                        title = "Question C goes Here",
                        description = "Excepteur sint occaecat cupidatat non proident, " +
                                "sunt in culpa qui officia deserunt mollit anim id est laborum."
                    )
                ),
                initialFaqItems = listOf()
            ),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(10.dp)
        )
    }
}

@ThemeModePreviews
@Composable
private fun FaqScreenEmptyPreview() {
    PreviewTheme {
        Content(
            state = State(listOf(), listOf()),
            effectFlow = Channel<Effect>().receiveAsFlow(),
            onEventSend = {},
            onNavigationRequested = {},
            paddingValues = PaddingValues(10.dp)
        )
    }
}