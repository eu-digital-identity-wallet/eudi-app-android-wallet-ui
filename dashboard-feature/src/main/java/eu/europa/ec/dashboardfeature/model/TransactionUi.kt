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

package eu.europa.ec.dashboardfeature.model

import eu.europa.ec.businesslogic.validator.model.FilterableItemPayload
import eu.europa.ec.corelogic.model.TransactionCategory
import eu.europa.ec.corelogic.model.TransactionLogData
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.wrap.ExpandableListItem
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class TransactionUi(
    val uiData: ExpandableListItem.SingleListItemData,
    val uiStatus: TransactionUiStatus,
    val transactionCategory: TransactionCategory,
) : FilterableItemPayload

enum class TransactionUiStatus {
    Completed, Failed;

    companion object {
        internal fun TransactionUiStatus.toUiText(resourceProvider: ResourceProvider): String {
            return when (this) {
                Completed -> resourceProvider.getString(R.string.transactions_filter_item_status_completed)
                Failed -> resourceProvider.getString(R.string.transactions_filter_item_status_failed)
            }
        }
    }
}

enum class TransactionUiType {
    PRESENTATION,
    ISSUANCE,
    SIGNING;
}

internal fun TransactionLog.Status.toTransactionUiStatus(): TransactionUiStatus {
    return when (this) {
        TransactionLog.Status.Incomplete, TransactionLog.Status.Error -> TransactionUiStatus.Failed
        TransactionLog.Status.Completed -> TransactionUiStatus.Completed
    }
}

internal fun TransactionLogData.toTransactionUiType(): TransactionUiType {
    return when (this) {
        is TransactionLogData.IssuanceLog -> TransactionUiType.ISSUANCE
        is TransactionLogData.PresentationLog -> TransactionUiType.PRESENTATION
        is TransactionLogData.SigningLog -> TransactionUiType.SIGNING
    }
}

//TODO once mocked Data is no longer needed
private const val FULL_DATETIME_PATTERN = "dd MMM yyyy hh:mm a"

//TODO once mocked Data is no longer needed
private val fullDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern(FULL_DATETIME_PATTERN)

// TODO should be replaced with actual transaction data
sealed interface Transaction {
    val id: String
    val name: String
    val status: String
    val creationDate: String
    val creationDateInstant: Instant
        get() = LocalDateTime.parse(
            creationDate, fullDateTimeFormatter
        ).toInstant(ZoneOffset.ofHours(3))

    data class AttestationPresentationTransaction(
        override val id: String,
        override val name: String,
        override val status: String,
        override val creationDate: String,
    ) : Transaction

    data class IssuanceTransaction(
        override val id: String,
        override val name: String,
        override val status: String,
        override val creationDate: String,
    ) : Transaction

    data class DocumentSigningTransaction(
        override val id: String,
        override val name: String,
        override val status: String,
        override val creationDate: String,
    ) : Transaction

    companion object {
        fun Transaction.getUiLabel(resourceProvider: ResourceProvider): String {
            return when (this) {
                is AttestationPresentationTransaction -> resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_presentation)
                is IssuanceTransaction -> resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_issuance)
                is DocumentSigningTransaction -> resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_signing)
            }
        }
    }
}