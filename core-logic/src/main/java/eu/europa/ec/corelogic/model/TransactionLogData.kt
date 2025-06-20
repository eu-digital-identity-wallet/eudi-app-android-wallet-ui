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

package eu.europa.ec.corelogic.model

import eu.europa.ec.corelogic.extension.getLocalizedDocumentName
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentedDocument
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

sealed interface TransactionLogData {

    val id: String
    val name: String
    val status: TransactionLog.Status
    val creationLocalDateTime: LocalDateTime
    val creationLocalDate: LocalDate

    data class PresentationLog(
        override val id: String,
        override val name: String,
        override val status: TransactionLog.Status,
        override val creationLocalDateTime: LocalDateTime,
        override val creationLocalDate: LocalDate,
        val relyingParty: TransactionLog.RelyingParty,
        val documents: List<PresentedDocument>,
    ) : TransactionLogData

    data class IssuanceLog(
        override val id: String,
        override val name: String,
        override val status: TransactionLog.Status,
        override val creationLocalDateTime: LocalDateTime,
        override val creationLocalDate: LocalDate,
    ) : TransactionLogData

    data class SigningLog(
        override val id: String,
        override val name: String,
        override val status: TransactionLog.Status,
        override val creationLocalDateTime: LocalDateTime,
        override val creationLocalDate: LocalDate,
    ) : TransactionLogData

    companion object {
        fun TransactionLogData.getTransactionTypeLabel(resourceProvider: ResourceProvider): String {
            return when (this) {
                is PresentationLog -> resourceProvider.getString(eu.europa.ec.resourceslogic.R.string.transactions_screen_filters_filter_by_transaction_type_presentation)
                is IssuanceLog -> resourceProvider.getString(eu.europa.ec.resourceslogic.R.string.transactions_screen_filters_filter_by_transaction_type_issuance)
                is SigningLog -> resourceProvider.getString(eu.europa.ec.resourceslogic.R.string.transactions_screen_filters_filter_by_transaction_type_signing)
            }
        }

        fun TransactionLogData.getTransactionDocumentNames(userLocale: Locale): List<String> {
            return when (this) {
                is IssuanceLog -> {
                    //TODO change this once Core supports more transaction types
                    emptyList()
                }

                is PresentationLog -> {
                    this.documents.mapNotNull { document ->
                        document.metadata.getLocalizedDocumentName(
                            userLocale = userLocale,
                            fallback = ""
                        ).takeIf { it.isNotBlank() }
                    }.flatMap { documentName ->
                        listOf(
                            documentName,
                            documentName.replace(regex = "\\s".toRegex(), replacement = "")
                        )
                    }
                }

                is SigningLog -> {
                    //TODO change this once Core supports more transaction types
                    emptyList()
                }
            }
        }
    }
}