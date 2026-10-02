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

package eu.europa.ec.dashboardfeature.ui.transactions.dpa_report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.dashboardfeature.ui.transactions.component.PrivacyActionExplanation
import eu.europa.ec.dashboardfeature.ui.transactions.component.PrivacyActionInfoCard
import eu.europa.ec.dashboardfeature.ui.transactions.dpa_report.model.DpaReportContactUi
import eu.europa.ec.dashboardfeature.ui.transactions.dpa_report.model.DpaReportUi
import eu.europa.ec.dashboardfeature.ui.transactions.openTransactionAction
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.IconDataUi
import eu.europa.ec.uilogic.component.ListItemDataUi
import eu.europa.ec.uilogic.component.ListItemLeadingContentDataUi
import eu.europa.ec.uilogic.component.ListItemMainContentDataUi
import eu.europa.ec.uilogic.component.ListItemTrailingContentDataUi
import eu.europa.ec.uilogic.component.ThemeColorKey
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ContentTitle
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.LargeTextPreviews
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.OncePerViewModelEffect
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.wrap.WrapListItem

@Composable
internal fun DpaReportScreen(
    navController: NavController,
    viewModel: DpaReportViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(Event.Pop) },
    ) { paddingValues ->
        Content(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(scrollState),
            state = state,
            onEventSend = { event -> viewModel.setEvent(event) },
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is Effect.Pop -> navController.popBackStack()
                is Effect.OpenChannel -> viewModel.setEvent(
                    Event.ChannelOpened(
                        actionId = effect.actionId,
                        opened = context.openTransactionAction(effect.url),
                    )
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
    onEventSend: (Event) -> Unit,
) {
    Column(modifier = modifier) {
        // Screen title.
        ContentTitle(title = stringResource(R.string.dpa_report_screen_title))

        state.report?.let { report ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SPACING_LARGE.dp),
            ) {
                // Responsible data protection authority.
                report.authority?.let { authority ->
                    PrivacyActionInfoCard(
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.dpa_report_authority_label),
                        text = authority,
                    )
                }

                // Wallet responsibility and external follow-up.
                PrivacyActionExplanation(
                    modifier = Modifier.fillMaxWidth(),
                    responsibility = report.responsibility,
                    followUp = report.followUp,
                )

                // Available contact methods for reporting.
                ReportContacts(
                    modifier = Modifier.fillMaxWidth(),
                    contacts = report.contacts,
                    enabled = !state.isLoading,
                    onContactClick = { url -> onEventSend(Event.ContactSelected(url)) },
                )
            }
        }
    }
}

@Composable
private fun ReportContacts(
    modifier: Modifier,
    contacts: List<DpaReportContactUi>,
    enabled: Boolean,
    onContactClick: (String) -> Unit,
) {
    Column(modifier = modifier) {
        contacts.forEach { contact ->
            ContactRow(
                modifier = Modifier.fillMaxWidth(),
                item = contact.item,
                enabled = enabled,
                onClick = { onContactClick(contact.url) },
            )
        }
    }
}

@Composable
private fun ContactRow(
    modifier: Modifier,
    item: ListItemDataUi,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    WrapListItem(
        modifier = modifier,
        item = item,
        onItemClick = if (enabled) {
            { onClick() }
        } else {
            null
        },
        mainContentVerticalPadding = SPACING_MEDIUM.dp,
        contentHorizontalPadding = 0.dp,
        mainContentTextStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.primary,
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    )
}

@ThemeModePreviews
@Composable
private fun DpaReportPreview(
    @PreviewParameter(DpaReportPreviewProvider::class) state: State,
) {
    val scrollState = rememberScrollState()
    PreviewTheme {
        ContentScreen(
            isLoading = state.isLoading,
            contentErrorConfig = state.error,
            navigatableAction = ScreenNavigateAction.BACKABLE,
            onBack = {},
        ) { paddingValues ->
            Content(
                modifier = Modifier
                    .padding(paddingValues)
                    .verticalScroll(scrollState),
                state = state,
                onEventSend = {},
            )
        }
    }
}

@LargeTextPreviews
@Composable
private fun WebsiteContactLargeTextPreview() {
    PreviewTheme {
        ContactRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SPACING_MEDIUM.dp),
            item = ListItemDataUi(
                itemId = "website",
                mainContentData = ListItemMainContentDataUi.Text(
                    "https://example.com/data-protection/transaction-reports",
                ),
                leadingContentData = ListItemLeadingContentDataUi.Icon(
                    iconData = AppIcons.Link,
                    tint = ThemeColorKey.OnSurfaceVariant,
                ),
                trailingContentData = ListItemTrailingContentDataUi.TextWithIcon(
                    text = "Visit website",
                    iconData = AppIcons.KeyboardArrowRight,
                ),
            ),
            enabled = true,
            onClick = {},
        )
    }
}

private class DpaReportPreviewProvider : PreviewParameterProvider<State> {
    override val values: Sequence<State>
        get() {
            val phone = previewContact(
                url = "tel:+302101234567",
                text = "+30 210 123 4567",
                icon = AppIcons.Call,
                actionText = "Call",
            )
            val email = previewContact(
                url = "mailto:privacy@example.com",
                text = "privacy@example.com",
                icon = AppIcons.Email,
                actionText = "Open email",
            )
            val website = previewContact(
                url = "https://example.com/data-protection/transaction-reports",
                text = "https://example.com/data-protection/transaction-reports",
                icon = AppIcons.Link,
                actionText = "Visit website",
            )
            val report = DpaReportUi(
                authority = "Example data protection authority — long authority name",
                responsibility = "<b>The wallet doesn't submit or track this report.</b> To report this transaction, contact the responsible data protection authority directly, using whichever method works best for you below.",
                followUp = "You will need to describe the transaction yourself. Any follow-up on the outcome will come from the data protection authority directly, not the wallet.",
                contacts = listOf(phone, email, website),
            )
            return sequenceOf(
                State(report = report),
                State(
                    report = report.copy(
                        authority = null,
                        contacts = listOf(email),
                    ),
                ),
                State(report = report, isLoading = true),
                State(isLoading = true),
                State(
                    report = report,
                    error = ContentErrorConfig(
                        errorSubTitle = "The communication channel could not be opened.",
                        onRetry = {},
                        onCancel = {},
                    ),
                ),
            )
        }

    private fun previewContact(
        url: String,
        text: String,
        icon: IconDataUi,
        actionText: String,
    ): DpaReportContactUi = DpaReportContactUi(
        item = ListItemDataUi(
            itemId = url,
            mainContentData = ListItemMainContentDataUi.Text(text),
            leadingContentData = ListItemLeadingContentDataUi.Icon(
                iconData = icon,
                tint = ThemeColorKey.OnSurfaceVariant,
            ),
            trailingContentData = ListItemTrailingContentDataUi.TextWithIcon(
                text = actionText,
                iconData = AppIcons.KeyboardArrowRight,
            ),
        ),
        url = url,
    )
}