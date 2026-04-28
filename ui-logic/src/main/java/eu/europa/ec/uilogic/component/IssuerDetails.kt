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

package eu.europa.ec.uilogic.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.WrapButton
import eu.europa.ec.uilogic.component.wrap.WrapExpandableCard
import eu.europa.ec.uilogic.component.wrap.WrapListItem
import java.net.URI

/**
 * Data class representing the UI state for the issuer details card.
 *
 * @property issuerName The name of the entity that issued the document.
 * @property issuerLogo The [URI] pointing to the issuer's logo image.
 * @property documentState The current state of the document (e.g., Issued or Revoked),
 * containing relevant dates.
 * @property isExpanded Boolean flag indicating whether the card is currently in its expanded state.
 */
data class IssuerDetailsCardDataUi(
    val issuerName: String?,
    val issuerLogo: URI?,
    val documentState: DocumentState,
    val isExpanded: Boolean,
) {
    sealed class DocumentState {
        data class Issued(
            val issuanceDate: String,
            val expirationDate: String?
        ) : DocumentState()

        data object Revoked : DocumentState()
    }

    /**
     * The string resource ID for the message text displayed when the card is expanded.
     * The value depends on whether the document is in an [DocumentState.Issued] or [DocumentState.Revoked] state.
     */
    val expandedMessageTextResId: Int
        @StringRes
        get() {
            return when (documentState) {
                is DocumentState.Issued -> {
                    R.string.document_details_issuer_card_issued_message_text
                }

                is DocumentState.Revoked -> {
                    R.string.document_details_issuer_card_revoked_message_text
                }
            }
        }

    /**
     * The string resource ID for the text displayed on the action button in the expanded state.
     * Returns a valid resource ID for [DocumentState.Issued] or null if the document is [DocumentState.Revoked].
     */
    val expandedActionButtonTextResId: Int?
        @StringRes
        get() {
            return when (documentState) {
                is DocumentState.Issued -> {
                    R.string.document_details_issuer_card_issued_action_btn_text
                }

                is DocumentState.Revoked -> {
                    null
                }
            }
        }
}

/**
 * A composable function that displays an issuer details card.
 *
 * This card shows information about an issuer, such as their logo, name,
 * and document status (e.g., Issued or Revoked). It supports an expandable
 * state to reveal additional details and action buttons.
 *
 * @param data The data object containing the issuer details and current state to display.
 * @param modifier Modifier used to adjust the layout or appearance of the card.
 * @param shape The shape of the card. Defaults to a rounded corner shape.
 * @param colors Optional [CardColors] to customize the card's background and content colors.
 * @param onExpandedChange Callback invoked when the expansion state is toggled.
 * @param onActionButtonClick Callback invoked when the action button in the expanded view is clicked.
 */
@Composable
fun IssuerDetailsCard(
    data: IssuerDetailsCardDataUi,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SIZE_SMALL.dp),
    colors: CardColors? = null,
    onExpandedChange: (() -> Unit),
    onActionButtonClick: (() -> Unit),
) {
    WrapExpandableCard(
        modifier = modifier,
        isExpanded = data.isExpanded,
        onExpandedChange = onExpandedChange,
        cardCollapsedContent = {
            val issuerLogoContentDescription =
                stringResource(R.string.content_description_issuer_logo_icon)

            val leadingContent = remember(data.issuerLogo) {
                data.issuerLogo?.let { safeIssuerLogo ->
                    ListItemLeadingContentDataUi.AsyncImage(
                        imageUrl = safeIssuerLogo.toString(),
                        contentDescription = issuerLogoContentDescription,
                        errorImage = AppIcons.Id,
                    )
                }
            }

            val (supportingText: String?, supportingTextColor: Color) = when (data.documentState) {
                is IssuerDetailsCardDataUi.DocumentState.Issued -> {
                    data.documentState.expirationDate?.let { safeExpirationDate ->
                        stringResource(
                            R.string.document_details_issuer_card_expires_on_text,
                            safeExpirationDate
                        )
                    } to MaterialTheme.colorScheme.onSurfaceVariant
                }

                is IssuerDetailsCardDataUi.DocumentState.Revoked -> {
                    stringResource(R.string.document_details_issuer_card_revoked_text) to MaterialTheme.colorScheme.error
                }
            }

            WrapListItem(
                modifier = Modifier.fillMaxWidth(),
                item = ListItemDataUi(
                    itemId = stringResource(R.string.document_details_issuer_card_id),
                    mainContentData = ListItemMainContentDataUi.Text(
                        text = data.issuerName.orEmpty()
                    ),
                    supportingText = supportingText,
                    leadingContentData = leadingContent,
                    trailingContentData = ListItemTrailingContentDataUi.Icon(
                        iconData = if (data.isExpanded)
                            AppIcons.KeyboardArrowUp
                        else AppIcons.KeyboardArrowDown
                    )
                ),
                onItemClick = {
                    onExpandedChange()
                },
                mainContentVerticalPadding = SPACING_MEDIUM.dp,
                supportingTextColor = supportingTextColor
            )
        },
        cardExpandedContent = {
            IssuerDetailsCardExpanded(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = SPACING_MEDIUM.dp,
                        bottom = SPACING_SMALL.dp,
                        start = SPACING_MEDIUM.dp,
                        end = SPACING_MEDIUM.dp,
                    ),
                data = data,
                onActionButtonClick = onActionButtonClick
            )
        },
        shape = shape,
        colors = colors,
        throttleClicks = true,
    )
}

@Composable
private fun IssuerDetailsCardExpanded(
    modifier: Modifier = Modifier,
    data: IssuerDetailsCardDataUi,
    onActionButtonClick: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
        horizontalAlignment = Alignment.Start
    ) {
        when (val docState = data.documentState) {
            is IssuerDetailsCardDataUi.DocumentState.Issued -> {
                Text(
                    text = stringResource(
                        R.string.document_details_issuer_card_issued_on_text,
                        docState.issuanceDate
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            is IssuerDetailsCardDataUi.DocumentState.Revoked -> {
                // Nothing to show
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = SPACING_EXTRA_SMALL.dp),
                text = stringResource(data.expandedMessageTextResId),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

            data.expandedActionButtonTextResId?.let { safeExpandedActionButtonTextResId ->
                WrapButton(
                    buttonConfig = ButtonConfig(
                        type = ButtonType.PRIMARY,
                        onClick = onActionButtonClick,
                        buttonColors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    )
                ) {
                    Text(text = stringResource(safeExpandedActionButtonTextResId))
                }
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun IssuerDetailsCardPreview() {
    PreviewTheme {
        var isExpanded by remember {
            mutableStateOf(false)
        }
        val issuerName = "Hellenic Government"
        val issuedState = IssuerDetailsCardDataUi.DocumentState.Issued(
            issuanceDate = "16 February 2024 - 13:18",
            expirationDate = "22 March 2030"
        )
        val revokedState = IssuerDetailsCardDataUi.DocumentState.Revoked

        val issuerDetailsItems = listOf(
            IssuerDetailsCardDataUi(
                issuerName = issuerName,
                issuerLogo = null,
                documentState = issuedState,
                isExpanded = false,
            ),
            IssuerDetailsCardDataUi(
                issuerName = issuerName,
                issuerLogo = null,
                documentState = issuedState,
                isExpanded = true,
            ),
            IssuerDetailsCardDataUi(
                issuerName = issuerName,
                issuerLogo = null,
                documentState = revokedState,
                isExpanded = false,
            ),
            IssuerDetailsCardDataUi(
                issuerName = issuerName,
                issuerLogo = null,
                documentState = revokedState,
                isExpanded = true,
            )
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
        ) {
            issuerDetailsItems.forEach { issuerDetailsItem ->
                IssuerDetailsCard(
                    data = issuerDetailsItem,
                    onExpandedChange = {
                        isExpanded = !isExpanded
                    },
                    onActionButtonClick = {}
                )
            }
        }
    }
}