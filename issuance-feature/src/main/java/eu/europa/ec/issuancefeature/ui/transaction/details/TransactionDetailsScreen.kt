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

package eu.europa.ec.issuancefeature.ui.transaction.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.commonfeature.model.TransactionDetailsDataSharedHolder
import eu.europa.ec.commonfeature.model.TransactionDetailsDataSignedHolder
import eu.europa.ec.commonfeature.model.TransactionDetailsUi
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
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.ExpandableListItemData
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.WrapButton
import eu.europa.ec.uilogic.component.wrap.WrapCard
import eu.europa.ec.uilogic.component.wrap.WrapChip
import eu.europa.ec.uilogic.component.wrap.WrapExpandableListItem
import eu.europa.ec.uilogic.component.wrap.WrapIcon
import eu.europa.ec.uilogic.component.wrap.WrapText
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
        modifier = Modifier.padding(
            PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = 0.dp,
                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                end = paddingValues.calculateStartPadding(LayoutDirection.Ltr)
            )
        )
    ) {
        state.title?.let { safeTitle ->
            ContentTitle(
                title = safeTitle,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SPACING_LARGE.dp)
        ) {
            TransactionDetailsCard(
                item = state.transactionDetailsCardData,
            )

            if (state.transactionDetailsUi?.transactionDetailsDataSharedList?.isNotEmpty() == true) {
                DataSharedDetails(
                    sectionTitle = state.detailsDataSharedSection,
                    dataSharedList = state.transactionDetailsUi.transactionDetailsDataSharedList,
                    onEventSend = onEventSend
                )
            }

            state.transactionDetailsUi?.transactionDetailsDataSigned?.dataSignedItems?.let { safeDataSignedItems ->
                DataSignedDetails(
                    sectionTitle = state.detailsDataSignedSection,
                    dataSignedList = safeDataSignedItems,
                    onEventSend = onEventSend
                )
            }

            ButtonsSection(
                onEventSend = onEventSend
            )
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

data class TransactionDetailsCardData(
    val transactionItemLabel: String,
    val transactionType: String,
    val relyingPartyName: String,
    val transactionDate: String,
    val status: String,
    val isVerified: Boolean = false
)

@Composable
private fun TransactionDetailsCard(
    modifier: Modifier = Modifier,
    item: TransactionDetailsCardData,
) {
    WrapCard(
        modifier = modifier,
        throttleClicks = true,
        shape = RoundedCornerShape(SIZE_SMALL.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SPACING_MEDIUM.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = item.transactionType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)
                ) {
                    Text(
                        text = item.transactionItemLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Normal
                    )
                    if (item.isVerified) {
                        WrapIcon(
                            iconData = AppIcons.Certified,
                            customTint = MaterialTheme.colorScheme.success
                        )
                    }
                }

                Text(
                    text = item.relyingPartyName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VSpacer.Medium()
                Text(
                    text = stringResource(R.string.transaction_details_screen_card_date_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.transactionDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            }

            WrapChip(
                modifier = Modifier,
                label = {
                    Text(
                        text = item.status,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                colors = InputChipDefaults.inputChipColors(
                    containerColor = MaterialTheme.colorScheme.success,
                    labelColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                border = null,
            )
        }
    }
}

@Composable
private fun DataSharedDetails(
    sectionTitle: String,
    onEventSend: (Event) -> Unit,
    dataSharedList: List<TransactionDetailsDataSharedHolder>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        SectionTitle(
            modifier = Modifier.fillMaxWidth(),
            text = sectionTitle,
        )

        dataSharedList.forEach { sharedAttestation ->
            var expandCollapseState by remember { mutableStateOf(false) }
            WrapExpandableListItem(
                data = ExpandableListItemData(
                    collapsed = ListItemData(
                        itemId = "0",
                        mainContentData = ListItemMainContentData.Text(text = "Digital ID"),
                        supportingText = "View details",
                        trailingContentData = ListItemTrailingContentData.Icon(
                            iconData = AppIcons.KeyboardArrowUp.takeIf { expandCollapseState }
                                ?: AppIcons.KeyboardArrowDown
                        )
                    ),
                    expanded = sharedAttestation.dataSharedItems
                ),
                onItemClick = { _ -> },
                modifier = Modifier.fillMaxWidth(),
                isExpanded = expandCollapseState,
                onExpandedChange = {
                    expandCollapseState = !expandCollapseState
                    onEventSend(
                        Event.ExpandOrCollapseTransactionDataSharedItem(itemId = "id")
                    )
                }
            )
        }
    }
}

@Composable
private fun DataSignedDetails(
    sectionTitle: String,
    onEventSend: (Event) -> Unit,
    dataSignedList: List<ListItemData>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        SectionTitle(
            modifier = Modifier.fillMaxWidth(),
            text = sectionTitle,
        )
        var expandCollapseState by remember { mutableStateOf(false) }
        WrapExpandableListItem(
            data = ExpandableListItemData(
                collapsed = ListItemData(
                    itemId = "0",
                    mainContentData = ListItemMainContentData.Text(text = "Signature details"),
                    supportingText = "View details",
                    trailingContentData = ListItemTrailingContentData.Icon(
                        iconData = AppIcons.KeyboardArrowUp.takeIf { expandCollapseState }
                            ?: AppIcons.KeyboardArrowDown
                    )
                ),
                expanded = dataSignedList
            ),
            onItemClick = { _ -> },
            modifier = Modifier.fillMaxWidth(),
            isExpanded = expandCollapseState,
            onExpandedChange = {
                expandCollapseState = !expandCollapseState
                onEventSend(
                    Event.ExpandOrCollapseTransactionDataSharedItem(itemId = "id")
                )
            }
        )
    }
}

@Composable
private fun ButtonsSection(onEventSend: (Event) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = SPACING_MEDIUM.dp
            ),
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)) {
            WrapText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.transaction_details_request_deletion_message),
                textConfig = TextConfig(
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
            )
            WrapButton(
                modifier = Modifier.fillMaxWidth(),
                buttonConfig = ButtonConfig(
                    type = ButtonType.SECONDARY,
                    onClick = { onEventSend(Event.PrimaryButtonPressed) },
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
            WrapText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.transaction_details_report_transaction_message),
                textConfig = TextConfig(
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
            )
            WrapButton(
                modifier = Modifier.fillMaxWidth(),
                buttonConfig = ButtonConfig(
                    type = ButtonType.SECONDARY,
                    onClick = { onEventSend(Event.SecondaryButtonPressed) },
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

@Composable
@ThemeModePreviews
private fun PreviewTransactionDetailsCard() {
    PreviewTheme {
        val transactionDetailsCardData = TransactionDetailsCardData(
            transactionType = "e-Signature/data sharing",
            transactionItemLabel = "File_signed.pdf or\nAttestation label",
            relyingPartyName = "SecureSign Inc.",
            transactionDate = "21 January 2025",
            status = "Completed",
            isVerified = true
        )

        TransactionDetailsCard(
            item = transactionDetailsCardData
        )
    }
}

@Composable
@ThemeModePreviews
private fun ContentPreview() {
    val items = listOf(
        ListItemData(
            itemId = "1",
            overlineText = "Family name",
            mainContentData = ListItemMainContentData.Text(text = "Doe"),
        ),
        ListItemData(
            itemId = "2",
            overlineText = "Given name",
            mainContentData = ListItemMainContentData.Text(text = "John"),
        )
    )
    val mockedDataSharedList = listOf(
        TransactionDetailsDataSharedHolder(dataSharedItems = items),
        TransactionDetailsDataSharedHolder(dataSharedItems = items),
    )
    val mockedDataSignedList = TransactionDetailsDataSignedHolder(
        dataSignedItems = items
    )
    val state = State(
        transactionDetailsCardData = TransactionDetailsCardData(
            transactionItemLabel = "File_signed.pdf",
            transactionType = "SecureSign Inc.",
            transactionDate = "21 January 2025",
            relyingPartyName = "Verisign",
            status = "Completed",
            isVerified = true
        ),
        transactionDetailsUi = TransactionDetailsUi(
            transactionId = "id",
            transactionName = "Transaction",
            transactionDetailsDataSharedList = mockedDataSharedList,
            transactionDetailsDataSigned = mockedDataSignedList
        ),
        detailsDataSharedSection = stringResource(R.string.transaction_details_data_shared),
        detailsDataSignedSection = stringResource(R.string.transaction_details_data_signed),
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