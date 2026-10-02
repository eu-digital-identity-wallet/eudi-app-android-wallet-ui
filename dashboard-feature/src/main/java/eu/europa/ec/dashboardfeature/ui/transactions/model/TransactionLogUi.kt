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

package eu.europa.ec.dashboardfeature.ui.transactions.model

import eu.europa.ec.corelogic.model.TransactionLogDomain
import eu.europa.ec.corelogic.model.TransactionResultDomain
import eu.europa.ec.dashboardfeature.ui.transactions.model.TransactionTypeUi.Companion.toUiText
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider

enum class TransactionStatusUi {
    Completed, NotCompleted;

    companion object {
        fun TransactionStatusUi.toUiText(resourceProvider: ResourceProvider): String {
            return when (this) {
                Completed -> resourceProvider.getString(R.string.transactions_filter_item_status_completed)
                NotCompleted -> resourceProvider.getString(R.string.transactions_filter_item_status_not_completed)
            }
        }
    }
}

enum class TransactionTypeUi {
    PRESENTATION,
    ISSUANCE,
    REISSUANCE,
    DELETION,
    SIGNING,
    DATA_DELETION_REQUEST,
    DPA_REPORT;

    companion object {
        fun TransactionTypeUi.toUiText(resourceProvider: ResourceProvider): String {
            return when (this) {
                PRESENTATION -> resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_presentation)
                ISSUANCE -> resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_issuance)
                REISSUANCE -> resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_reissuance)
                DELETION -> resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_deletion)
                SIGNING -> resourceProvider.getString(R.string.transactions_screen_filters_filter_by_transaction_type_signing)
                DATA_DELETION_REQUEST -> resourceProvider.getString(R.string.transaction_type_data_deletion_request)
                DPA_REPORT -> resourceProvider.getString(R.string.transaction_type_dpa_report)
            }
        }
    }
}

fun TransactionResultDomain.toTransactionStatusUi(): TransactionStatusUi {
    return when (this) {
        is TransactionResultDomain.Completed -> TransactionStatusUi.Completed
        is TransactionResultDomain.NotCompleted -> TransactionStatusUi.NotCompleted
    }
}

fun TransactionLogDomain.toTransactionTypeUi(): TransactionTypeUi {
    return when (this) {
        is TransactionLogDomain.Presentation -> TransactionTypeUi.PRESENTATION
        is TransactionLogDomain.CredentialIssuance -> TransactionTypeUi.ISSUANCE
        is TransactionLogDomain.CredentialReissuance -> TransactionTypeUi.REISSUANCE
        is TransactionLogDomain.CredentialDeletion -> TransactionTypeUi.DELETION
        is TransactionLogDomain.SigningSealing -> TransactionTypeUi.SIGNING
        is TransactionLogDomain.DataDeletionRequest -> TransactionTypeUi.DATA_DELETION_REQUEST
        is TransactionLogDomain.DpaReport -> TransactionTypeUi.DPA_REPORT
    }
}

/**
 * Who the transaction was with, or what it was about when there is no other party. A transaction
 * that names neither is titled by its type.
 */
fun TransactionLogDomain.toTransactionTitle(resourceProvider: ResourceProvider): String {
    val name = when (this) {
        is TransactionLogDomain.Presentation -> toTransactionPartyName()

        is TransactionLogDomain.CredentialIssuance ->
            toTransactionPartyName() ?: details.credentials.firstOrNull()?.identifier

        is TransactionLogDomain.CredentialReissuance ->
            toTransactionPartyName() ?: details.credentials.firstOrNull()?.identifier

        is TransactionLogDomain.CredentialDeletion ->
            toTransactionPartyName()
                ?: credential.identifier.takeIf { identifier -> identifier.isNotBlank() }

        is TransactionLogDomain.SigningSealing -> toTransactionPartyName()
            ?: fileName?.takeIf { name -> name.isNotBlank() }

        is TransactionLogDomain.DataDeletionRequest -> toTransactionPartyName()
        is TransactionLogDomain.DpaReport -> toTransactionPartyName()
    }
    return name ?: toTransactionTypeUi().toUiText(resourceProvider)
}

fun TransactionLogDomain.toTransactionPartyName(): String? {
    val name = when (this) {
        is TransactionLogDomain.Presentation -> party.name
        is TransactionLogDomain.CredentialIssuance -> details.issuer.name
        is TransactionLogDomain.CredentialReissuance -> details.issuer.name
        is TransactionLogDomain.CredentialDeletion -> issuer.name
        is TransactionLogDomain.SigningSealing -> service.name
        is TransactionLogDomain.DataDeletionRequest -> party.name
        is TransactionLogDomain.DpaReport -> dpaName
    }
    return name?.text?.takeIf { text -> text.isNotBlank() }
}

fun TransactionLogDomain.isVisibleInTransactionList(): Boolean {
    return when (this) {
        is TransactionLogDomain.Presentation,
        is TransactionLogDomain.CredentialIssuance,
        is TransactionLogDomain.CredentialReissuance,
        is TransactionLogDomain.CredentialDeletion,
        is TransactionLogDomain.SigningSealing -> true

        is TransactionLogDomain.DataDeletionRequest,
        is TransactionLogDomain.DpaReport -> false
    }
}

fun TransactionLogDomain.toTransactionSearchTags(): List<String> {
    val tags = when (this) {
        is TransactionLogDomain.Presentation -> listOfNotNull(
            toTransactionPartyName(),
            intermediary?.name?.text
        )

        is TransactionLogDomain.SigningSealing -> listOfNotNull(
            toTransactionPartyName(),
            fileName
        )

        is TransactionLogDomain.CredentialDeletion -> listOfNotNull(
            toTransactionPartyName(),
            credential.identifier,
        )

        is TransactionLogDomain.CredentialIssuance,
        is TransactionLogDomain.CredentialReissuance -> listOfNotNull(
            toTransactionPartyName()
        )

        is TransactionLogDomain.DataDeletionRequest,
        is TransactionLogDomain.DpaReport -> emptyList()
    }

    return tags
        .filter { tag -> tag.isNotBlank() }
        .distinct()
}