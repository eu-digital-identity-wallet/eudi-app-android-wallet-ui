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

package eu.europa.ec.dashboardfeature.ui.transactions.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.dashboardfeature.model.TransactionDetailsCardData
import eu.europa.ec.dashboardfeature.model.TransactionDetailsDataSharedHolder
import eu.europa.ec.dashboardfeature.model.TransactionDetailsDataSignedHolder
import eu.europa.ec.dashboardfeature.model.TransactionDetailsUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.success
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.SectionTitle
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.TextLengthPreviewProvider
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem
import eu.europa.ec.uilogic.component.wrap.WrapButton
import eu.europa.ec.uilogic.component.wrap.WrapCard
import eu.europa.ec.uilogic.component.wrap.WrapChip
import eu.europa.ec.uilogic.component.wrap.WrapExpandableListItem
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach

@Composable
internal fun TransactionDetailsScreen(
    navController: NavController,
    viewModel: TransactionDetailsViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(Event.Pop) },
    ) { paddingValues ->
        Content(
            state = state,
            onEventSend = { viewModel.setEvent(it) },
            effectFlow = viewModel.effect,
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(navigationEffect, navController)
            },
            paddingValues = paddingValues,
        )
    }

    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.Init)
    }
}

@Composable
private fun Content(
    state: State,
    onEventSend: (Event) -> Unit,
    effectFlow: Flow<Effect>,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                paddingValues = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 0.dp,
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
                )
            )
    ) {
        ContentTitle(title = state.title)

        state.transactionDetailsUi?.let { safeTransactionDetailsUi ->
            TransactionDetailsCard(
                modifier = Modifier.fillMaxWidth(),
                item = safeTransactionDetailsUi.transactionDetailsCardData
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SPACING_LARGE.dp),
            verticalArrangement = Arrangement.spacedBy(SPACING_LARGE.dp)
        ) {

            state.transactionDetailsUi?.transactionDetailsDataShared?.let { safeTransactionDetailsDataShared ->
                ExpandableDataSection(
                    modifier = Modifier.fillMaxWidth(),
                    sectionTitle = stringResource(R.string.transaction_details_data_shared_section_title),
                    dataItems = safeTransactionDetailsDataShared.dataSharedItems,
                    onEventSend = onEventSend,
                )
            }

            state.transactionDetailsUi?.transactionDetailsDataSigned?.dataSignedItems?.let { safeDataSignedItems ->
                ExpandableDataSection(
                    modifier = Modifier.fillMaxWidth(),
                    sectionTitle = stringResource(R.string.transaction_details_data_signed_section_title),
                    dataItems = safeDataSignedItems,
                    onEventSend = onEventSend,
                )
            }

            state.transactionDetailsUi?.let { safeTransactionDetailsUi ->
                if (safeTransactionDetailsUi.transactionDetailsDataShared.dataSharedItems.isNotEmpty()
                    || safeTransactionDetailsUi.transactionDetailsDataSigned?.dataSignedItems?.isNotEmpty() == true
                ) {
                    ButtonsSection(onEventSend = onEventSend)
                }
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

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController,
) {
    when (navigationEffect) {
        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute) {
                popUpTo(navigationEffect.popUpToScreenRoute) {
                    inclusive = navigationEffect.inclusive
                }
            }
        }

        is Effect.Navigation.Pop -> navController.popBackStack()
    }
}


@Composable
private fun TransactionDetailsCard(
    modifier: Modifier = Modifier,
    item: TransactionDetailsCardData,
) {
    WrapCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SPACING_MEDIUM.dp),
            verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)
        ) {
            Text(
                text = item.transactionTypeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp, Alignment.Start)
            ) {
                item.relyingPartyName?.let { safeRelyingPartyName ->
                    Text(
                        modifier = Modifier
                            .wrapContentWidth()
                            .weight(weight = 1f, fill = false),
                        text = safeRelyingPartyName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (item.relyingPartyIsVerified == true) {
                    WrapIcon(
                        iconData = AppIcons.Certified,
                        customTint = MaterialTheme.colorScheme.success
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.transaction_details_screen_card_date_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.transactionDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                WrapChip(
                    modifier = Modifier,
                    label = {
                        Text(
                            text = item.transactionStatusLabel,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(
                        containerColor = when (item.transactionIsCompleted) {
                            true -> MaterialTheme.colorScheme.success
                            false -> MaterialTheme.colorScheme.error
                        },
                        labelColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    border = null,
                )
            }
        }
    }
}

@Composable
private fun ExpandableDataSection(
    modifier: Modifier,
    sectionTitle: String,
    dataItems: List<ExpandableListItem.NestedListItemData>,
    onEventSend: (Event) -> Unit
) {
    if (dataItems.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
        ) {
            SectionTitle(
                modifier = Modifier.fillMaxWidth(),
                text = sectionTitle,
            )

            dataItems.forEach { sharedDocument ->
                WrapExpandableListItem(
                    modifier = Modifier.fillMaxWidth(),
                    header = sharedDocument.header,
                    data = sharedDocument.nestedItems,
                    onItemClick = null,
                    onExpandedChange = {
                        onEventSend(
                            Event.ExpandOrCollapseGroupItem(itemId = it.itemId)
                        )
                    },
                    isExpanded = sharedDocument.isExpanded,
                    collapsedMainContentVerticalPadding = SPACING_MEDIUM.dp,
                    expandedMainContentVerticalPadding = SPACING_MEDIUM.dp,
                )
            }
        }
    }
}

@Composable
private fun ButtonsSection(onEventSend: (Event) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.transaction_details_request_deletion_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WrapButton(
                modifier = Modifier.fillMaxWidth(),
                buttonConfig = ButtonConfig(
                    type = ButtonType.SECONDARY,
                    enabled = false,
                    onClick = { onEventSend(Event.RequestDataDeletionPressed) },
                    isWarning = true,
                )
            ) {
                Text(
                    text = stringResource(R.string.transaction_details_request_deletion_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.transaction_details_report_transaction_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WrapButton(
                modifier = Modifier.fillMaxWidth(),
                buttonConfig = ButtonConfig(
                    type = ButtonType.SECONDARY,
                    enabled = false,
                    onClick = { onEventSend(Event.ReportSuspiciousTransactionPressed) },
                    isWarning = false,
                )
            ) {
                Text(
                    text = stringResource(R.string.transaction_details_report_transaction_button),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun PreviewTransactionDetailsCompletedCard(
    @PreviewParameter(TextLengthPreviewProvider::class) text: String
) {
    PreviewTheme {
        val transactionDetailsCardData = TransactionDetailsCardData(
            transactionTypeLabel = "Data sharing",
            relyingPartyName = "RP name $text",
            transactionDate = "21 January 2025",
            transactionStatusLabel = "Completed",
            transactionIsCompleted = true,
            relyingPartyIsVerified = true,
        )

        TransactionDetailsCard(
            item = transactionDetailsCardData
        )
    }
}

@ThemeModePreviews
@Composable
private fun PreviewTransactionDetailsFailedCard(
    @PreviewParameter(TextLengthPreviewProvider::class) text: String
) {
    PreviewTheme {
        val transactionDetailsCardData = TransactionDetailsCardData(
            transactionTypeLabel = "Data sharing",
            relyingPartyName = "RP name $text",
            transactionDate = "21 January 2025",
            transactionStatusLabel = "Failed",
            transactionIsCompleted = false,
            relyingPartyIsVerified = true,
        )

        TransactionDetailsCard(
            item = transactionDetailsCardData
        )
    }
}

@ThemeModePreviews
@Composable
private fun ContentPreview() {
    val items = listOf(
        ExpandableListItem.NestedListItemData(
            header = ListItemData(
                itemId = "0",
                mainContentData = ListItemMainContentData.Text(text = "Digital ID"),
                supportingText = "View Details",
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.KeyboardArrowDown
                ),
            ),
            nestedItems = listOf(
                ExpandableListItem.SingleListItemData(
                    ListItemData(
                        itemId = "1",
                        overlineText = "Family name",
                        mainContentData = ListItemMainContentData.Text(text = "Doe"),
                    )
                ),
                ExpandableListItem.SingleListItemData(
                    ListItemData(
                        itemId = "2",
                        overlineText = "Given name",
                        mainContentData = ListItemMainContentData.Text(text = "John"),
                    )
                )
            ),
            isExpanded = true
        )
    )
    val mockedDataSharedList = TransactionDetailsDataSharedHolder(dataSharedItems = items)
    val mockedDataSignedList = TransactionDetailsDataSignedHolder(dataSignedItems = items)

    val state = State(
        title = stringResource(R.string.transaction_details_screen_title),
        transactionDetailsUi = TransactionDetailsUi(
            transactionId = "id",
            transactionDetailsCardData = TransactionDetailsCardData(
                transactionTypeLabel = "Presentation",
                transactionDate = "21 January 2025",
                relyingPartyName = "Verisign",
                transactionStatusLabel = "Completed",
                transactionIsCompleted = true,
                relyingPartyIsVerified = true,
            ),
            transactionDetailsDataShared = mockedDataSharedList,
            transactionDetailsDataSigned = mockedDataSignedList
        )
    )

    PreviewTheme {
        Content(
            state = state,
            onEventSend = {},
            effectFlow = emptyFlow(),
            onNavigationRequested = {},
            paddingValues = PaddingValues(SPACING_MEDIUM.dp)
        )
    }
}