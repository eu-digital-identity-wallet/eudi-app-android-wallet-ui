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

package eu.europa.ec.commonfeature.ui.transaction_details.transformer

import eu.europa.ec.commonfeature.model.TransactionDetailsDataSharedHolder
import eu.europa.ec.commonfeature.model.TransactionDetailsDataSignedHolder
import eu.europa.ec.commonfeature.model.TransactionDetailsUi
import eu.europa.ec.commonfeature.ui.transaction_details.domain.TransactionClaimItem
import eu.europa.ec.commonfeature.ui.transaction_details.domain.TransactionDetailsDomain
import eu.europa.ec.commonfeature.util.keyIsPortrait
import eu.europa.ec.commonfeature.util.keyIsSignature
import eu.europa.ec.eudi.wallet.document.ElementIdentifier
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.ListItemData
import eu.europa.ec.uilogic.component.ListItemLeadingContentData
import eu.europa.ec.uilogic.component.ListItemMainContentData
import eu.europa.ec.uilogic.component.ListItemTrailingContentData
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem

fun TransactionDetailsDomain.transformToTransactionDetailsUi(): TransactionDetailsUi {
    val sharedDataList: List<TransactionDetailsDataSharedHolder> = listOf(
        TransactionDetailsDataSharedHolder(
            dataSharedItems = ExpandableListItem.NestedListItemData(
                header = ListItemData(
                    itemId = "01",
                    mainContentData = ListItemMainContentData.Text(text = "Digital ID"),
                    supportingText = "View Details",
                    trailingContentData = ListItemTrailingContentData.Icon(
                        iconData = AppIcons.KeyboardArrowDown
                    )
                ),
                nestedItems = this.sharedDataClaimItems.toExpandableListItem(),
                isExpanded = false
            )
        )
    )

    val signedData = TransactionDetailsDataSignedHolder(
        dataSignedItems = ExpandableListItem.NestedListItemData(
            header = ListItemData(
                itemId = "02",
                mainContentData = ListItemMainContentData.Text(text = "Signature details"),
                supportingText = "View Details",
                trailingContentData = ListItemTrailingContentData.Icon(
                    iconData = AppIcons.KeyboardArrowDown
                )
            ),
            nestedItems = this.signedDataClaimItems.toExpandableListItem(),
            isExpanded = false
        )
    )

    return TransactionDetailsUi(
        transactionId = this.transactionId,
        transactionName = this.transactionName,
        transactionIdentifier = "identifier",
        transactionDetailsDataSharedList = sharedDataList,
        transactionDetailsDataSigned = signedData,
    )
}

private fun List<TransactionClaimItem>.toExpandableListItem(): List<ExpandableListItem> {
    return this
        .sortedBy { it.readableName.lowercase() }
        .map {
            val mainContent = when {
                keyIsPortrait(key = it.elementIdentifier.toString()) -> {
                    ListItemMainContentData.Text(text = "")
                }

                keyIsSignature(key = it.elementIdentifier.toString()) -> {
                    ListItemMainContentData.Image(base64Image = it.value)
                }

                else -> {
                    ListItemMainContentData.Text(text = it.value)
                }
            }

            val itemId = generateUniqueFieldId(
                elementIdentifier = it.elementIdentifier.toString(),
                transactionId = it.transactionId
            )

            val leadingContent = if (keyIsPortrait(key = it.elementIdentifier.toString())) {
                ListItemLeadingContentData.UserImage(userBase64Image = it.value)
            } else {
                null
            }

            ExpandableListItem.SingleListItemData(
                header = ListItemData(
                    itemId = itemId,
                    mainContentData = mainContent,
                    overlineText = it.readableName,
                    leadingContentData = leadingContent
                )
            )
        }
}

fun generateUniqueFieldId(
    elementIdentifier: ElementIdentifier,
    transactionId: String,
): String =
    elementIdentifier + transactionId