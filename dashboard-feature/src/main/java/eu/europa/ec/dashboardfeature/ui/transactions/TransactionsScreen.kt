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

package eu.europa.ec.dashboardfeature.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.corelogic.model.TransactionCategory
import eu.europa.ec.dashboardfeature.model.SearchItem
import eu.europa.ec.dashboardfeature.model.TransactionUi
import eu.europa.ec.dashboardfeature.ui.FiltersSearchBar
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.SectionTitle
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.WrapIconButton
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import eu.europa.ec.uilogic.component.wrap.WrapListItems

typealias DashboardEvent = eu.europa.ec.dashboardfeature.ui.dashboard.Event
typealias ShowSideMenuEvent = eu.europa.ec.dashboardfeature.ui.dashboard.Event.SideMenu.Show

@Composable
internal fun TransactionsScreen(
    navHostController: NavController,
    viewModel: TransactionsViewModel,
    onDashboardEventSent: (DashboardEvent) -> Unit,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ContentScreen(
            isLoading = false,
            navigatableAction = ScreenNavigateAction.NONE,
            topBar = {
                TopBar(
                    onEventSend = { event ->
                        viewModel.setEvent(event)
                    },
                    onDashboardEventSent = onDashboardEventSent
                )
            }
        ) { paddingValues ->
            Content(
                state = state,
                onEventSend = {
                    viewModel.setEvent(it)
                },
                paddingValues = paddingValues,
            )
        }
    }
}

@Composable
private fun Content(
    state: State,
    onEventSend: (Event) -> Unit,
    paddingValues: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                paddingValues = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 0.dp,
                    start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                    end = paddingValues.calculateEndPadding(LayoutDirection.Ltr)
                )
            ),
        contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding()),
    ) {
        item {
            val searchItem =
                SearchItem(searchLabel = stringResource(R.string.transactions_screen_search_label))
            FiltersSearchBar(
                placeholder = searchItem.searchLabel,
                onValueChange = { onEventSend(Event.OnSearchQueryChanged(it)) },
                onFilterClick = { onEventSend(Event.FiltersPressed) },
                onClearClick = { onEventSend(Event.OnSearchQueryChanged("")) },
                isFilteringActive = state.isFilteringActive,
                text = state.searchText
            )
            VSpacer.Large()
        }

        if (state.showNoResultsFound) {
            item {
                NoResults(modifier = Modifier.fillMaxWidth())
            }
        } else {
            itemsIndexed(items = state.transactionsUi) { index, (documentCategory, documents) ->
                TransactionCategory(
                    modifier = Modifier.fillMaxWidth(),
                    category = documentCategory,
                    transactions = documents,
                    onEventSend = onEventSend
                )

                if (index != state.transactionsUi.lastIndex) {
                    VSpacer.ExtraLarge()
                }
            }
        }
    }

    OneTimeLaunchedEffect {
        onEventSend(Event.Init)
    }
}

@Composable
private fun TransactionCategory(
    modifier: Modifier = Modifier,
    category: TransactionCategory,
    transactions: List<TransactionUi>,
    onEventSend: (Event) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        SectionTitle(
            modifier = Modifier.fillMaxWidth(),
            text = category.displayName ?: stringResource(category.stringResId)
        )

        WrapListItems(
            items = transactions.map { it.uiData },
            onItemClick = { item ->
                onEventSend(
                    Event.TransactionItemPressed(itemId = item.itemId)
                )
            }
        )
    }
}

@Composable
private fun NoResults(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        WrapListItem(
            item = ListItemData(
                itemId = stringResource(R.string.transactions_screen_search_no_results_id),
                mainContentData = ListItemMainContentData.Text(text = stringResource(R.string.transactions_screen_search_no_results)),
            ),
            onItemClick = null,
            modifier = Modifier.fillMaxWidth(),
            mainContentVerticalPadding = SPACING_MEDIUM.dp,
        )
    }
}

@Composable
private fun TopBar(
    onEventSend: (Event) -> Unit,
    onDashboardEventSent: (DashboardEvent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SPACING_SMALL.dp,
                vertical = SPACING_MEDIUM.dp
            )
    ) {
        WrapIconButton(
            modifier = Modifier.align(Alignment.CenterStart),
            iconData = AppIcons.Menu,
            customTint = MaterialTheme.colorScheme.onSurface,
        ) {
            onDashboardEventSent(ShowSideMenuEvent)
        }

        Text(
            modifier = Modifier.align(Alignment.Center),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium,
            text = stringResource(R.string.transactions_screen_title)
        )

        WrapIconButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            iconData = AppIcons.Export,
            customTint = MaterialTheme.colorScheme.onSurface,
        ) {
            // transactions exporting action
        }
    }
}

@ThemeModePreviews
@Composable
private fun TransactionsScreenPreview() {
    ContentScreen(
        isLoading = false,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { },
        topBar = {
            TopBar(
                onEventSend = { },
                onDashboardEventSent = {}
            )
        },
    ) {}
}