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

package eu.europa.ec.dashboardfeature.ui.transactions.data_deletion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.dashboardfeature.ui.transactions.component.PrivacyActionExplanation
import eu.europa.ec.dashboardfeature.ui.transactions.component.PrivacyActionInfoCard
import eu.europa.ec.dashboardfeature.ui.transactions.data_deletion.model.DataDeletionRequestUi
import eu.europa.ec.dashboardfeature.ui.transactions.openTransactionAction
import eu.europa.ec.resourceslogic.R
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
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.WrapButton
import eu.europa.ec.uilogic.extension.paddingFrom

@Composable
internal fun DataDeletionRequestScreen(
    navController: NavController,
    viewModel: DataDeletionRequestViewModel,
) {
    val state: State by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    ContentScreen(
        isLoading = state.isLoading,
        contentErrorConfig = state.error,
        navigatableAction = ScreenNavigateAction.BACKABLE,
        onBack = { viewModel.setEvent(Event.Pop) },
        stickyBottom = { paddingValues ->
            state.request?.let { request ->
                ContinueButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .paddingFrom(
                            pv = paddingValues,
                            bottom = false,
                        ),
                    text = request.buttonText,
                    enabled = !state.isLoading,
                    onClick = { viewModel.setEvent(Event.Continue) },
                )
            }
        },
    ) { paddingValues ->
        Content(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(scrollState),
            state = state,
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
) {
    Column(modifier = modifier) {
        // Screen title.
        ContentTitle(title = stringResource(R.string.data_deletion_screen_title))

        state.request?.let { request ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SPACING_LARGE.dp),
            ) {
                // Explanation of the selected contact method.
                PrivacyActionInfoCard(
                    modifier = Modifier.fillMaxWidth(),
                    label = null,
                    text = request.description,
                )

                // Wallet responsibility, data retention and follow-up.
                PrivacyActionExplanation(
                    modifier = Modifier.fillMaxWidth(),
                    responsibility = request.responsibility,
                    followUp = request.retentionNotice,
                )
            }
        }
    }
}

@Composable
private fun ContinueButton(
    modifier: Modifier,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    WrapButton(
        modifier = modifier,
        buttonConfig = ButtonConfig(
            type = ButtonType.PRIMARY,
            enabled = enabled,
            onClick = onClick,
        ),
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}

@ThemeModePreviews
@Composable
private fun DataDeletionRequestPreview(
    @PreviewParameter(DataDeletionPreviewProvider::class) state: State,
) {
    val scrollState = rememberScrollState()
    PreviewTheme {
        ContentScreen(
            isLoading = state.isLoading,
            contentErrorConfig = state.error,
            navigatableAction = ScreenNavigateAction.BACKABLE,
            onBack = {},
            stickyBottom = { paddingValues ->
                state.request?.let { request ->
                    ContinueButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(paddingValues),
                        text = request.buttonText,
                        enabled = !state.isLoading,
                        onClick = {},
                    )
                }
            },
        ) { paddingValues ->
            Content(
                modifier = Modifier
                    .padding(paddingValues)
                    .verticalScroll(scrollState),
                state = state,
            )
        }
    }
}

@LargeTextPreviews
@Composable
private fun ContinueButtonLargeTextPreview() {
    PreviewTheme {
        ContinueButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SPACING_MEDIUM.dp),
            text = "Call An organisation with a very long recorded name",
            enabled = true,
            onClick = {},
        )
    }
}

private class DataDeletionPreviewProvider : PreviewParameterProvider<State> {
    override val values: Sequence<State>
        get() {
            val website = DataDeletionRequestUi(
                description = "You're about to leave the wallet and go to TravelBook’s website, where you can fill in a form to request deletion of the data you shared.",
                responsibility = "<b>The wallet doesn't send or manage this request for you.</b> Once you're on the form, it's up to you to complete and submit it.",
                retentionNotice = "TravelBook may still be required to keep some of your data for legal reasons, even after your request. Any updates on the outcome will come from TravelBook directly, not the wallet.",
                buttonText = "Continue to TravelBook’s website",
                contactUrl = "https://example.com/privacy",
            )
            val email = website.copy(
                description = "This will open your email application with a message ready to send to Relying party, so you can request deletion of the data you shared.",
                responsibility = "<b>The wallet doesn't send this email or manage the request for you.</b> You'll need to review, complete, and send it yourself.",
                retentionNotice = "Relying party may still be required to keep some of your data for legal reasons, even after your request. Any updates on the outcome will come from Relying party directly, not the wallet.",
                buttonText = "Open email to Relying party",
                contactUrl = "mailto:privacy@example.com",
            )
            val phone = website.copy(
                description = "This will call An organisation with a very long recorded name directly, so you can request deletion of the data you shared.",
                responsibility = "<b>The wallet doesn't make this request for you.</b> You'll need to explain what you're asking for yourself once you're on the call.",
                retentionNotice = "An organisation with a very long recorded name may still be required to keep some of your data for legal reasons, even after your request. Any updates on the outcome will come from An organisation with a very long recorded name directly, not the wallet.",
                buttonText = "Call An organisation with a very long recorded name",
                contactUrl = "tel:+302101234567",
            )
            return sequenceOf(
                State(request = website),
                State(request = email),
                State(request = phone),
                State(request = website, isLoading = true),
                State(isLoading = true),
                State(
                    error = ContentErrorConfig(
                        errorSubTitle = "The communication channel could not be opened.",
                        onRetry = {},
                        onCancel = {},
                    ),
                    request = website,
                ),
            )
        }
}