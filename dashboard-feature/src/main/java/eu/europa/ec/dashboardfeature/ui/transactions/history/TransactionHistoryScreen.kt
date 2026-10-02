/*
 * Copyright (c) 2026 European Commission
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

package eu.europa.ec.dashboardfeature.ui.transactions.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.dashboardfeature.ui.transactions.component.PrivacyActionInfoCard
import eu.europa.ec.dashboardfeature.ui.transactions.history.model.TransactionHistoryUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemSupportingContentDataUi
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.OncePerViewModelEffect
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import eu.europa.ec.uilogic.component.wrap.WrapListItemDefaults
import eu.europa.ec.uilogic.navigation.DashboardScreens

@Composable
internal fun TransactionHistoryScreen(
    navController: NavController,
    viewModel: TransactionHistoryViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(Event.Pop) },
    ) { paddingValues ->
        Content(
            modifier = Modifier.fillMaxSize(),
            state = state,
            paddingValues = paddingValues,
            listState = listState,
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is Effect.Pop -> navController.popBackStack()
                is Effect.PopToDashboard -> navController.popBackStack(
                    route = DashboardScreens.Dashboard.screenRoute,
                    inclusive = false,
                )
            }
        }
    }

    OncePerViewModelEffect(viewModel) {
        viewModel.setEvent(Event.Init)
    }
}

@Composable
private fun Content(
    modifier: Modifier,
    state: State,
    paddingValues: PaddingValues,
    listState: LazyListState,
) {
    state.history?.let { history ->
        LazyColumn(
            modifier = modifier,
            state = listState,
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(WrapListItemDefaults.GroupedItemSpacing),
        ) {
            // Screen title and explanation, with the recorded authority for reports.
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = SPACING_LARGE.dp),
                ) {
                    ContentTitle(title = history.title)

                    HistoryExplanation(
                        modifier = Modifier.fillMaxWidth(),
                        disclaimer = history.disclaimer,
                        authority = history.authority,
                        introduction = history.introduction,
                    )
                }
            }

            // Recorded communication methods and attempt dates.
            itemsIndexed(
                items = history.items,
                key = { _, attempt -> attempt.itemId },
            ) { index, attempt ->
                HistoryRow(
                    modifier = Modifier.fillMaxWidth(),
                    item = attempt,
                    shape = WrapListItemDefaults.groupedShape(
                        index = index,
                        itemCount = history.items.size
                    ),
                )
            }
        }
    }
}

@Composable
private fun HistoryExplanation(
    modifier: Modifier,
    disclaimer: String,
    authority: String?,
    introduction: String,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
    ) {
        Text(
            text = disclaimer,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        authority?.let { recordedAuthority ->
            PrivacyActionInfoCard(
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.dpa_report_authority_label),
                text = recordedAuthority,
            )
        }
        Text(
            text = introduction,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun HistoryRow(
    modifier: Modifier,
    item: ListItemDataUi,
    shape: Shape,
) {
    WrapListItem(
        modifier = modifier,
        item = item,
        onItemClick = null,
        mainContentVerticalPadding = 12.dp,
        shape = shape,
    )
}

@ThemeModePreviews
@Composable
private fun TransactionHistoryPreview(
    @PreviewParameter(TransactionHistoryPreviewProvider::class) state: State,
) {
    val listState = rememberLazyListState()
    PreviewTheme {
        ContentScreen(
            isLoading = state.isLoading,
            contentErrorConfig = state.error,
            navigatableAction = ScreenNavigateAction.BACKABLE,
            onBack = {},
        ) { paddingValues ->
            Content(
                modifier = Modifier.fillMaxSize(),
                state = state,
                paddingValues = paddingValues,
                listState = listState,
            )
        }
    }
}

private class TransactionHistoryPreviewProvider : PreviewParameterProvider<State> {
    override val values: Sequence<State>
        get() {
            val email = previewItem(
                id = "email",
                method = "Email",
                date = "24 December 2024 - 22:01",
            )
            val website = previewItem(
                id = "website",
                method = "Web form",
                date = "22 December 2024 - 17:32",
            )
            val deletionHistory = TransactionHistoryUi(
                title = "Previous data deletion requests for TravelBook",
                disclaimer = "Data deletion requests are completed outside the Wallet; therefore their final outcome cannot be tracked.",
                authority = null,
                introduction = "You have previously attempted to initiate a data deletion request with this Relying Party via the following methods:",
                items = listOf(
                    previewItem(
                        id = "phone",
                        method = "Phone call",
                        date = "25 December 2024 - 09:15",
                    ),
                    email,
                    website,
                ),
            )
            val reportHistory = TransactionHistoryUi(
                title = "Previous transaction reports for TravelBook",
                disclaimer = "Transaction reports are completed outside the Wallet; therefore their final outcome cannot be tracked.",
                authority = "Autorité de la protection des données - Gegevensbeschermingsautoriteit (APD-GBA)",
                introduction = "You have previously attempted to initiate a transaction report with the responsible data protection authority via the following methods:",
                items = listOf(
                    email,
                    website.copy(
                        mainContentData = ListItemMainContentDataUi.Text("Website"),
                    ),
                ),
            )
            return sequenceOf(
                State(history = deletionHistory),
                State(history = reportHistory),
                State(
                    history = deletionHistory.copy(
                        title = "Previous data deletion requests for Relying party",
                        items = listOf(email),
                    ),
                ),
                State(
                    history = reportHistory.copy(
                        authority = null,
                        items = listOf(email),
                    ),
                ),
                State(
                    history = reportHistory.copy(
                        title = "Previous transaction reports for Example relying party with a long recorded name",
                    ),
                ),
                State(
                    history = deletionHistory.copy(
                        introduction = "No previous data deletion request attempts for this transaction.",
                        items = emptyList(),
                    ),
                ),
                State(
                    history = reportHistory.copy(
                        authority = null,
                        introduction = "No previous transaction report attempts for this transaction.",
                        items = emptyList(),
                    ),
                ),
                State(isLoading = true),
                State(
                    error = ContentErrorConfig(
                        errorSubTitle = "History could not be loaded.",
                        onRetry = {},
                        onCancel = {},
                    ),
                ),
            )
        }

    private fun previewItem(
        id: String,
        method: String,
        date: String,
    ): ListItemDataUi = ListItemDataUi(
        itemId = id,
        mainContentData = ListItemMainContentDataUi.Text(method),
        supportingContentData = ListItemSupportingContentDataUi.Text(
            text = date,
            maxLines = Int.MAX_VALUE,
        ),
    )
}