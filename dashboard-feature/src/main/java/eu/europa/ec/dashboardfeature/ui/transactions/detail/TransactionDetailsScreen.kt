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

package eu.europa.ec.dashboardfeature.ui.transactions.detail

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.PresentationActionCountsUiState
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionContactUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDataProtectionAction
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsBodyUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsCardUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsFieldUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsGroupUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsItemUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsMetadataUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsSectionUi
import eu.europa.ec.dashboardfeature.ui.transactions.detail.model.TransactionDetailsUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.success
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemSupportingContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.SectionTitle
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.content.ToolbarActionUi
import eu.europa.ec.uilogic.component.content.ToolbarConfig
import eu.europa.ec.uilogic.component.preview.LargeTextPreviews
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.OncePerViewModelEffect
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.BottomSheetTextDataUi
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.DialogBottomSheet
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemUi
import eu.europa.ec.uilogic.component.wrap.WrapButton
import eu.europa.ec.uilogic.component.wrap.WrapCard
import eu.europa.ec.uilogic.component.wrap.WrapChip
import eu.europa.ec.uilogic.component.wrap.WrapExpandableCard
import eu.europa.ec.uilogic.component.wrap.WrapExpandableListItem
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import eu.europa.ec.uilogic.component.wrap.WrapModalBottomSheet
import eu.europa.ec.uilogic.component.wrap.WrapTextButton
import eu.europa.ec.uilogic.extension.openUrl
import eu.europa.ec.uilogic.extension.paddingFrom
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionDetailsScreen(
    navController: NavController,
    viewModel: TransactionDetailsViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val details = state.transactionDetailsUi

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        toolBarConfig = getToolbarConfig(
            showDelete = state.error == null && details != null,
            enabled = !state.isLoading,
            onDeleteClick = { viewModel.setEvent(Event.DeletePressed) },
        ),
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(Event.Pop) },
    ) { paddingValues ->
        Content(
            modifier = Modifier
                .paddingFrom(paddingValues, bottom = false)
                .verticalScroll(scrollState)
                .navigationBarsPadding(),
            state = state,
            onEventSend = { event -> viewModel.setEvent(event) },
        )

        if (state.isBottomSheetOpen) {
            WrapModalBottomSheet(
                onDismissRequest = {
                    viewModel.setEvent(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                },
                sheetState = bottomSheetState,
            ) {
                SheetContent(
                    sheetContent = state.sheetContent,
                    onConfirm = { viewModel.setEvent(Event.BottomSheet.Delete.PrimaryButtonPressed) },
                    onCancel = { viewModel.setEvent(Event.BottomSheet.Delete.SecondaryButtonPressed) },
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is Effect.Navigation -> handleNavigationEffect(effect, navController, context)
                is Effect.ShowBottomSheet -> {
                    viewModel.setEvent(Event.BottomSheet.UpdateBottomSheetState(isOpen = true))
                }

                is Effect.CloseBottomSheet -> {
                    scope.launch {
                        bottomSheetState.hide()
                    }.invokeOnCompletion {
                        if (!bottomSheetState.isVisible) {
                            viewModel.setEvent(Event.BottomSheet.UpdateBottomSheetState(isOpen = false))
                        }
                    }
                }
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
    onEventSend: (Event) -> Unit,
) {
    val details = state.transactionDetailsUi
    val presentation = details?.body as? TransactionDetailsBodyUi.Presentation

    Column(modifier = modifier) {
        // Screen title.
        ContentTitle(title = state.title)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SPACING_MEDIUM.dp),
            verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
        ) {
            // Transaction summary and expandable metadata.
            details?.transactionDetailsCardUi?.let { summary ->
                TransactionDetailsCard(
                    modifier = Modifier.fillMaxWidth(),
                    item = summary,
                    isExpanded = state.isCardExpanded,
                    onExpandedChange = { onEventSend(Event.ExpandOrCollapseCard) },
                    onLinkClick = { url -> onEventSend(Event.OpenLink(url)) },
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SPACING_LARGE.dp),
            ) {
                // Requested/shared data, credential identifiers or the signing filename.
                details?.body?.sections.orEmpty().forEach { section ->
                    DetailsSection(
                        section = section,
                        expandedGroupIds = state.expandedGroupIds,
                        onGroupExpandedChange = { id ->
                            onEventSend(Event.ExpandOrCollapseGroupItem(id))
                        },
                        onLinkClick = { url -> onEventSend(Event.OpenLink(url)) },
                    )
                }

                presentation?.let { body ->
                    // Presentation history loading/error status and retry.
                    HistoryStatus(
                        state = body.actionCounts,
                        isLoading = state.isLoading,
                        onRetry = { onEventSend(Event.RetryPresentationActionCounts) },
                    )

                    // Deletion requests and transaction reports, with their history links.
                    PresentationActionSections(
                        counts = body.actionCounts,
                        canRequestDeletion = body.deletionContacts.isNotEmpty(),
                        canReport = body.reportContacts.isNotEmpty(),
                        isLoading = state.isLoading,
                        onDeletionHistoryClick = {
                            onEventSend(Event.HistoryPressed(TransactionDataProtectionAction.RequestDataDeletion))
                        },
                        onReportHistoryClick = {
                            onEventSend(Event.HistoryPressed(TransactionDataProtectionAction.ReportSuspiciousTransaction))
                        },
                        onRequestDeletionClick = { onEventSend(Event.RequestDataDeletionPressed) },
                        onReportClick = { onEventSend(Event.ReportSuspiciousTransactionPressed) },
                    )
                }
            }
        }
    }
}

private fun getToolbarConfig(
    showDelete: Boolean,
    enabled: Boolean,
    onDeleteClick: () -> Unit,
): ToolbarConfig = ToolbarConfig(
    actions = if (showDelete) {
        listOf(
            ToolbarActionUi(
                icon = AppIcons.Delete,
                enabled = enabled,
                onClick = onDeleteClick,
            )
        )
    } else {
        emptyList()
    }
)

@Composable
private fun SheetContent(
    sheetContent: TransactionDetailsBottomSheetContent,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    when (sheetContent) {
        is TransactionDetailsBottomSheetContent.DeleteTransactionConfirmation -> DialogBottomSheet(
            textData = BottomSheetTextDataUi(
                title = stringResource(R.string.transaction_details_bottom_sheet_delete_title),
                message = stringResource(R.string.transaction_details_bottom_sheet_delete_subtitle),
                positiveButtonText = stringResource(R.string.transaction_details_delete_button),
                negativeButtonText = stringResource(R.string.generic_cancel),
                isPositiveButtonWarning = true,
            ),
            leadingIcon = AppIcons.DeleteFilled,
            leadingIconTint = MaterialTheme.colorScheme.error,
            onPositiveClick = onConfirm,
            onNegativeClick = onCancel,
        )
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController,
    context: Context,
) {
    when (navigationEffect) {
        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute) {
                launchSingleTop = true
            }
        }

        is Effect.Navigation.Pop -> navController.popBackStack()
        is Effect.Navigation.OpenUrlExternally -> context.openUrl(navigationEffect.url.toUri())
    }
}

@Composable
private fun TransactionDetailsCard(
    modifier: Modifier,
    item: TransactionDetailsCardUi,
    isExpanded: Boolean,
    onExpandedChange: () -> Unit,
    onLinkClick: (String) -> Unit,
) {
    WrapExpandableCard(
        modifier = modifier,
        isExpanded = isExpanded && item.metadata.isNotEmpty(),
        onExpandedChange = null,
        cardCollapsedContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = SPACING_MEDIUM.dp,
                        start = SPACING_MEDIUM.dp,
                        end = SPACING_MEDIUM.dp,
                        bottom = if (item.metadata.isEmpty()) SPACING_SMALL.dp else 0.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
            ) {
                TransactionPartySummary(
                    transactionType = item.transactionTypeLabel,
                    partyName = item.partyName,
                    providerType = item.providerType,
                )
                TransactionDateAndStatus(
                    date = item.transactionDate,
                    status = item.transactionStatusLabel,
                    isCompleted = item.transactionIsCompleted,
                )
                item.nonCompletionReason?.let { reason ->
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (item.metadata.isNotEmpty()) {
                    WrapTextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(
                            if (isExpanded) R.string.generic_hide_details
                            else R.string.generic_view_details,
                        ),
                        enabled = true,
                        trailingIcon = null,
                        onClick = onExpandedChange,
                    )
                }
            }
        },
        cardExpandedContent = {
            TransactionMetadataPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = SPACING_MEDIUM.dp,
                        top = SPACING_SMALL.dp,
                        end = SPACING_MEDIUM.dp,
                        bottom = SPACING_MEDIUM.dp,
                    ),
                groups = item.metadata,
                onLinkClick = onLinkClick,
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = null,
        throttleClicks = true,
    )
}

@Composable
private fun TransactionPartySummary(
    transactionType: String,
    partyName: String?,
    providerType: String?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
    ) {
        Text(
            text = transactionType,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        partyName?.let { name ->
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        providerType?.let { type ->
            Text(
                text = type,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TransactionDateAndStatus(
    date: String,
    status: String,
    isCompleted: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.transaction_details_screen_card_date_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TransactionStatusChip(
            label = status,
            isCompleted = isCompleted
        )
    }
}

@Composable
private fun TransactionStatusChip(
    modifier: Modifier = Modifier,
    label: String,
    isCompleted: Boolean
) {
    WrapChip(
        modifier = modifier,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        colors = InputChipDefaults.inputChipColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.success
            else MaterialTheme.colorScheme.error,
            labelColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = null,
    )
}

@Composable
private fun TransactionMetadataPanel(
    modifier: Modifier,
    groups: List<TransactionDetailsMetadataUi>,
    onLinkClick: (String) -> Unit,
) {
    WrapCard(
        modifier = modifier,
        shape = RoundedCornerShape(SIZE_SMALL.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceDim
        ),
    ) {
        groups.forEachIndexed { index, group ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = SPACING_MEDIUM.dp)
                )
            }
            TransactionMetadataGroup(
                group = group,
                onLinkClick = onLinkClick
            )
        }
    }
}

@Composable
private fun TransactionMetadataGroup(
    group: TransactionDetailsMetadataUi,
    onLinkClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        group.fields.forEach { field ->
            TransactionDetailField(
                item = field.item,
                onClick = field.url?.let { url ->
                    { onLinkClick(url) }
                },
            )
        }
    }
}

@Composable
private fun TransactionDetailField(
    item: ListItemDataUi,
    onClick: (() -> Unit)?,
) {
    WrapListItem(
        modifier = Modifier.fillMaxWidth(),
        item = item,
        onItemClick = onClick?.let { onFieldClick ->
            { onFieldClick() }
        },
        mainContentVerticalPadding = SPACING_MEDIUM.dp,
        mainContentTextStyle = MaterialTheme.typography.bodyMedium.copy(
            color = if (onClick != null) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        overlineTextStyle = {
            MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceDim
        ),
    )
}

@Composable
private fun DetailsSection(
    section: TransactionDetailsSectionUi,
    expandedGroupIds: Set<String>,
    onGroupExpandedChange: (String) -> Unit,
    onLinkClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
    ) {
        SectionTitle(
            modifier = Modifier.fillMaxWidth(),
            text = section.title
        )

        section.emptyItem?.let { emptyItem ->
            WrapListItem(
                modifier = Modifier.fillMaxWidth(),
                item = emptyItem,
                onItemClick = null,
                mainContentVerticalPadding = SPACING_MEDIUM.dp,
                mainContentTextStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceDim,
                ),
            )
        }

        section.items.forEach { row ->
            WrapListItem(
                modifier = Modifier.fillMaxWidth(),
                item = row.item,
                onItemClick = row.url?.let { url ->
                    { onLinkClick(url) }
                },
                mainContentVerticalPadding = SPACING_MEDIUM.dp,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceDim
                ),
            )
        }

        section.groups.forEach { group ->
            CredentialGroup(
                group = group,
                isExpanded = group.header.itemId in expandedGroupIds,
                onExpandedChange = { onGroupExpandedChange(group.header.itemId) },
            )
        }
    }
}

@Composable
private fun CredentialGroup(
    group: TransactionDetailsGroupUi,
    isExpanded: Boolean,
    onExpandedChange: () -> Unit,
) {
    WrapExpandableListItem(
        modifier = Modifier.fillMaxWidth(),
        header = group.header,
        data = group.items,
        onItemClick = null,
        isItemClickable = { true },
        onExpandedChange = { onExpandedChange() },
        isExpanded = isExpanded,
        collapsedMainContentVerticalPadding = SPACING_MEDIUM.dp,
        expandedMainContentVerticalPadding = SPACING_MEDIUM.dp,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceDim),
    )
}

@Composable
private fun HistoryStatus(
    state: PresentationActionCountsUiState,
    isLoading: Boolean,
    onRetry: () -> Unit,
) {
    when (state) {
        is PresentationActionCountsUiState.Loading -> Text(
            text = stringResource(R.string.privacy_history_loading),
            style = MaterialTheme.typography.bodyMedium,
        )

        is PresentationActionCountsUiState.Content -> Unit

        is PresentationActionCountsUiState.Failure -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp),
            ) {
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                WrapButton(
                    modifier = Modifier.fillMaxWidth(),
                    buttonConfig = ButtonConfig(
                        type = ButtonType.SECONDARY,
                        enabled = !isLoading,
                        onClick = onRetry,
                        isWarning = false,
                    ),
                ) {
                    Text(text = stringResource(R.string.privacy_history_retry))
                }
            }
        }
    }
}

@Composable
private fun PresentationActionSections(
    counts: PresentationActionCountsUiState,
    canRequestDeletion: Boolean,
    canReport: Boolean,
    isLoading: Boolean,
    onDeletionHistoryClick: () -> Unit,
    onReportHistoryClick: () -> Unit,
    onRequestDeletionClick: () -> Unit,
    onReportClick: () -> Unit,
) {
    val content = counts as? PresentationActionCountsUiState.Content
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SPACING_EXTRA_LARGE.dp),
    ) {
        PresentationActionSection(
            title = stringResource(R.string.transaction_details_request_deletion_section_title),
            description = stringResource(R.string.transaction_details_request_deletion_message),
            historyLabel = if (content != null && content.dataDeletionRequests > 0) {
                stringResource(R.string.privacy_history_deletion_link, content.dataDeletionRequests)
            } else {
                null
            },
            buttonLabel = stringResource(R.string.transaction_details_request_deletion_button),
            isHistoryEnabled = !isLoading,
            isActionEnabled = !isLoading && canRequestDeletion,
            isWarning = true,
            onHistoryClick = onDeletionHistoryClick,
            onInitiateClick = onRequestDeletionClick,
        )
        PresentationActionSection(
            title = stringResource(R.string.transaction_details_report_transaction_section_title),
            description = stringResource(R.string.transaction_details_report_transaction_message),
            historyLabel = if (content != null && content.dpaReports > 0) {
                stringResource(R.string.privacy_history_report_link, content.dpaReports)
            } else {
                null
            },
            buttonLabel = stringResource(R.string.transaction_details_report_transaction_button),
            isHistoryEnabled = !isLoading,
            isActionEnabled = !isLoading && canReport,
            isWarning = false,
            onHistoryClick = onReportHistoryClick,
            onInitiateClick = onReportClick,
        )
    }
}

@Composable
private fun PresentationActionSection(
    title: String,
    description: String,
    historyLabel: String?,
    buttonLabel: String,
    isHistoryEnabled: Boolean,
    isActionEnabled: Boolean,
    isWarning: Boolean,
    onHistoryClick: () -> Unit,
    onInitiateClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
    ) {
        SectionTitle(
            modifier = Modifier.fillMaxWidth(),
            text = title
        )

        Column {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            historyLabel?.let { text ->
                WrapTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = text,
                    enabled = isHistoryEnabled,
                    contentPadding = PaddingValues(vertical = 10.dp),
                    contentAlignment = Alignment.Start,
                    shape = RectangleShape,
                    trailingIcon = AppIcons.KeyboardArrowRight,
                    isRippleEnabled = false,
                    onClick = onHistoryClick,
                )
            }
        }

        WrapButton(
            modifier = Modifier.fillMaxWidth(),
            buttonConfig = ButtonConfig(
                type = ButtonType.SECONDARY,
                enabled = isActionEnabled,
                onClick = onInitiateClick,
                isWarning = isWarning,
            ),
        ) {
            Text(
                text = buttonLabel,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@ThemeModePreviews
@Composable
private fun DeleteTransactionConfirmationPreview() {
    PreviewTheme {
        SheetContent(
            sheetContent = TransactionDetailsBottomSheetContent.DeleteTransactionConfirmation,
            onConfirm = {},
            onCancel = {},
        )
    }
}

@ThemeModePreviews
@Composable
private fun TransactionDetailsPreview(
    @PreviewParameter(TransactionDetailsPreviewProvider::class) state: State,
) {
    PreviewTheme {
        Content(
            modifier = Modifier
                .padding(SPACING_MEDIUM.dp)
                .verticalScroll(rememberScrollState()),
            state = state,
            onEventSend = {},
        )
    }
}

@LargeTextPreviews
@Composable
private fun TransactionDetailsCardLargeTextPreview() {
    PreviewTheme {
        TransactionDetailsCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SPACING_MEDIUM.dp),
            item = TransactionDetailsCardUi(
                transactionTypeLabel = "Presentation",
                transactionStatusLabel = "Not completed",
                transactionIsCompleted = false,
                transactionDate = "16 Feb 2024 01:18 PM",
                partyName = "Example relying party",
                providerType = "ServiceProvider",
                nonCompletionReason = "The connection was interrupted.",
                metadata = emptyList(),
            ),
            isExpanded = false,
            onExpandedChange = {},
            onLinkClick = {},
        )
    }
}

private class TransactionDetailsPreviewProvider : PreviewParameterProvider<State> {
    override val values: Sequence<State>
        get() {
            val contact =
                TransactionContactUi("https://example.com/support", "https://example.com/support")
            val presentation = TransactionDetailsBodyUi.Presentation(
                requested = previewClaims("requested", "DATA REQUESTED", isExpanded = false),
                shared = previewClaims("shared", "DATA SHARED", isExpanded = false),
                transactionData = null,
                deletionContacts = listOf(contact),
                reportContacts = listOf(contact),
                actionCounts = PresentationActionCountsUiState.Content(2, 2),
            )
            val contacts = TransactionDetailsMetadataUi(
                fields = listOf(
                    TransactionDetailsFieldUi("party:contact:0", "Contact", "EU", null),
                    TransactionDetailsFieldUi(
                        "party:contact:1",
                        "Contact",
                        contact.label,
                        contact.url
                    ),
                    TransactionDetailsFieldUi(
                        "party:contact:2",
                        "Contact",
                        contact.label,
                        contact.url
                    ),
                ),
            )
            val card = TransactionDetailsCardUi(
                transactionTypeLabel = "Presentation",
                transactionStatusLabel = "Completed",
                transactionIsCompleted = true,
                transactionDate = "16 Feb 2024 01:18 PM",
                partyName = "TravelBook",
                providerType = "ServiceProvider",
                nonCompletionReason = null,
                metadata = listOf(
                    TransactionDetailsMetadataUi(
                        listOf(
                            TransactionDetailsFieldUi(
                                "purpose", "Intended use",
                                "We will use your identity and age to verify you for a new current account. Your data will be used once to complete onboarding and to meet anti-money laundering requirements.",
                                null,
                            ),
                        ),
                    ),
                    TransactionDetailsMetadataUi(
                        listOf(
                            TransactionDetailsFieldUi(
                                "privacy:0", "Privacy policy",
                                "https://data.europa.eu/eudi/policy/trust-service-practice-statement",
                                "https://data.europa.eu/eudi/policy/trust-service-practice-statement",
                            ),
                        ),
                    ),
                    contacts,
                    TransactionDetailsMetadataUi(
                        listOf(
                            TransactionDetailsFieldUi(
                                "intermediary:name",
                                "Intermediary name",
                                "Verifier Signer dev",
                                null
                            )
                        ),
                    ),
                ),
            )
            val base = State(
                title = "Transaction information",
                transactionDetailsUi = TransactionDetailsUi("presentation", card, presentation),
            )
            val empty = presentation.copy(
                requested = TransactionDetailsSectionUi(
                    title = "DATA REQUESTED",
                    items = emptyList(),
                    groups = emptyList(),
                    emptyItem = ListItemDataUi(
                        itemId = "requested:empty",
                        mainContentData = ListItemMainContentDataUi.Text("No data requested"),
                    ),
                ),
                shared = TransactionDetailsSectionUi(
                    title = "DATA SHARED",
                    items = emptyList(),
                    groups = emptyList(),
                    emptyItem = ListItemDataUi(
                        itemId = "shared:empty",
                        mainContentData = ListItemMainContentDataUi.Text("No data shared"),
                    ),
                ),
                deletionContacts = emptyList(),
                reportContacts = emptyList(),
                actionCounts = PresentationActionCountsUiState.Content(0, 0),
            )
            val states = listOf(
                base,
                base.copy(
                    transactionDetailsUi = TransactionDetailsUi(
                        "presentation",
                        card,
                        presentation.copy(
                            requested = previewClaims(
                                "requested",
                                "DATA REQUESTED",
                                isExpanded = true
                            ),
                        ),
                    ),
                    isCardExpanded = true,
                    expandedGroupIds = setOf("requested:0"),
                ),
                base.copy(
                    transactionDetailsUi = TransactionDetailsUi(
                        "empty",
                        card.copy(partyName = null, providerType = null, metadata = emptyList()),
                        empty,
                    ),
                ),
                base.copy(
                    transactionDetailsUi = TransactionDetailsUi(
                        "presentation",
                        card,
                        presentation.copy(requested = empty.requested),
                    ),
                ),
                base.copy(
                    transactionDetailsUi = TransactionDetailsUi(
                        "presentation",
                        card,
                        presentation.copy(shared = empty.shared),
                    ),
                ),
                base.copy(
                    transactionDetailsUi = TransactionDetailsUi(
                        "failed",
                        card.copy(
                            partyName = "An organisation with a very long recorded name that must wrap without truncating",
                            transactionStatusLabel = "Not completed",
                            transactionIsCompleted = false,
                            nonCompletionReason = "The requested data could not be shared because the connection was interrupted.",
                        ),
                        presentation,
                    ),
                ),
            )
            val credentials =
                previewFields("CREDENTIALS", null, "urn:eu.europa.ec.eudi:learning:credential:1")
            val issuedCredentials = credentials.copy(title = "CREDENTIALS ISSUED")
            val issuanceCounts = TransactionDetailsMetadataUi(
                fields = listOf(
                    TransactionDetailsFieldUi(
                        "requested-count",
                        "Credentials requested",
                        "1",
                        null
                    ),
                    TransactionDetailsFieldUi("issued-count", "Credentials issued", "1", null),
                ),
            )
            val otherTypes = listOf(
                Triple(
                    "Issuance",
                    TransactionDetailsBodyUi.Issuance(issuedCredentials),
                    listOf(issuanceCounts, contacts)
                ),
                Triple(
                    "Re-issuance",
                    TransactionDetailsBodyUi.Reissuance(issuedCredentials),
                    listOf(
                        issuanceCounts,
                        TransactionDetailsMetadataUi(
                            listOf(
                                TransactionDetailsFieldUi(
                                    "trigger",
                                    "Initiated by",
                                    "The Wallet",
                                    null
                                )
                            )
                        ),
                        contacts,
                    ),
                ),
                Triple("Deletion", TransactionDetailsBodyUi.Deletion(credentials), emptyList()),
                Triple(
                    "Signing",
                    TransactionDetailsBodyUi.Signing(
                        previewFields(
                            "DATA SIGNED",
                            "File name",
                            "fileXX.pdf"
                        )
                    ),
                    emptyList(),
                ),
                Triple(
                    "Signing",
                    TransactionDetailsBodyUi.Signing(null),
                    listOf(
                        TransactionDetailsMetadataUi(
                            listOf(
                                TransactionDetailsFieldUi(
                                    "signing-transaction",
                                    "Signing transaction identifier",
                                    "signing-123",
                                    null
                                )
                            )
                        )
                    ),
                ),
            ).map { (type, body, metadata) ->
                base.copy(
                    transactionDetailsUi = TransactionDetailsUi(
                        type,
                        card.copy(
                            transactionTypeLabel = type,
                            partyName = "Example organisation",
                            providerType = when (type) {
                                "Deletion" -> null
                                "Signing" -> "ESigESealCreationProvider"
                                else -> "PIDProvider"
                            },
                            metadata = metadata,
                        ),
                        body,
                    ),
                    isCardExpanded = true,
                )
            }
            val histories = listOf(
                PresentationActionCountsUiState.Content(1, 0),
                PresentationActionCountsUiState.Content(0, 1),
                PresentationActionCountsUiState.Loading,
                PresentationActionCountsUiState.Failure("Previous attempts could not be loaded."),
            ).map { counts ->
                base.copy(
                    transactionDetailsUi = TransactionDetailsUi(
                        "presentation", card,
                        presentation.copy(actionCounts = counts),
                    ),
                )
            }
            val independentActions = listOf(
                presentation.copy(deletionContacts = emptyList()),
                presentation.copy(reportContacts = emptyList()),
            ).map { body ->
                base.copy(transactionDetailsUi = TransactionDetailsUi("presentation", card, body))
            }
            return (states + otherTypes + histories + independentActions + base.copy(isLoading = true)).asSequence()
        }

    private fun previewFields(
        title: String,
        label: String?,
        value: String
    ): TransactionDetailsSectionUi {
        return TransactionDetailsSectionUi(
            title = title,
            items = listOf(
                TransactionDetailsItemUi(
                    item = ListItemDataUi(
                        itemId = title,
                        overlineText = label,
                        mainContentData = ListItemMainContentDataUi.Text(value),
                    ),
                    url = null,
                ),
            ),
            groups = emptyList(),
            emptyItem = null,
        )
    }

    private fun previewClaims(
        prefix: String,
        title: String,
        isExpanded: Boolean,
    ): TransactionDetailsSectionUi {
        return TransactionDetailsSectionUi(
            title = title,
            items = emptyList(),
            groups = listOf(
                TransactionDetailsGroupUi(
                    header = ListItemDataUi(
                        itemId = "$prefix:0",
                        mainContentData = ListItemMainContentDataUi.Text("eu.europa.ec.eudi.pid.1"),
                        supportingContentData = ListItemSupportingContentDataUi.Text("View details"),
                        trailingContentData = ListItemTrailingContentDataUi.Icon(
                            if (isExpanded) AppIcons.KeyboardArrowUp else AppIcons.KeyboardArrowDown,
                        ),
                    ),
                    items = listOf("family_name", "given_name").mapIndexed { index, claim ->
                        ExpandableListItemUi.SingleListItem(
                            header = ListItemDataUi(
                                itemId = "$prefix:0:$index",
                                mainContentData = ListItemMainContentDataUi.Text("[\"eu.europa.ec.eudi.pid.1\"][\"$claim\"]"),
                            ),
                        )
                    },
                ),
            ),
            emptyItem = null,
        )
    }
}

@ThemeModePreviews
@Composable
private fun RecordedSigningRequestPreview(
    @PreviewParameter(RecordedSigningRequestPreviewProvider::class) preview: RecordedSigningRequestPreviewData,
) {
    PreviewTheme {
        DetailsSection(
            section = preview.section,
            expandedGroupIds = preview.expandedGroupIds,
            onGroupExpandedChange = {},
            onLinkClick = {},
        )
    }
}

private data class RecordedSigningRequestPreviewData(
    val section: TransactionDetailsSectionUi,
    val expandedGroupIds: Set<String>,
)

private class RecordedSigningRequestPreviewProvider :
    PreviewParameterProvider<RecordedSigningRequestPreviewData> {
    override val values: Sequence<RecordedSigningRequestPreviewData>
        get() = sequenceOf(
            signingRequestPreview(isExpanded = false, isUnavailable = false),
            signingRequestPreview(isExpanded = true, isUnavailable = false),
            signingRequestPreview(isExpanded = true, isUnavailable = true),
        )

    private fun signingRequestPreview(
        isExpanded: Boolean,
        isUnavailable: Boolean,
    ): RecordedSigningRequestPreviewData {
        val groupId = "transaction-data"
        val fields = if (isUnavailable) {
            listOf(null to "Details for this transaction are unavailable.")
        } else {
            listOf(
                "Trust framework" to "eIDAS",
                null to "Transaction 1",
                "Requested credentials" to "query_0",
                "Number of signatures" to "1",
                "Document" to "file-sample_150kB.pdf",
                "DTBSR hash" to "jDudt7/CCgNAacyhuKo4A6aiDzUYUG5MWQibmUGMPXg=",
                "DTBSR hash algorithm" to "SHA-256",
                null to "Transaction 2",
                "Document" to "Document 1",
                "Signature type" to "Qualified electronic seal",
                "Signature format" to "PAdES",
                "Conformance level" to "AdES-B-LT",
                "Document location" to "https://documents.example/contract.pdf?reference=recorded",
                "One-time password (OTP)" to "000123",
                "Response URI" to "https://signer.example/response",
            )
        }
        return RecordedSigningRequestPreviewData(
            section = TransactionDetailsSectionUi(
                title = "SIGNING REQUEST",
                items = emptyList(),
                groups = listOf(
                    TransactionDetailsGroupUi(
                        header = ListItemDataUi(
                            itemId = groupId,
                            mainContentData = ListItemMainContentDataUi.Text("Signature details"),
                            supportingContentData = ListItemSupportingContentDataUi.Text("View details"),
                            trailingContentData = ListItemTrailingContentDataUi.Icon(
                                iconData = if (isExpanded) AppIcons.KeyboardArrowUp else AppIcons.KeyboardArrowDown,
                            ),
                        ),
                        items = fields.mapIndexed { index, (label, value) ->
                            ExpandableListItemUi.SingleListItem(
                                header = ListItemDataUi(
                                    itemId = "$groupId/field-$index",
                                    overlineText = label,
                                    mainContentData = ListItemMainContentDataUi.Text(value),
                                ),
                            )
                        },
                    )
                ),
                emptyItem = null,
            ),
            expandedGroupIds = if (isExpanded) setOf(groupId) else emptySet(),
        )
    }
}