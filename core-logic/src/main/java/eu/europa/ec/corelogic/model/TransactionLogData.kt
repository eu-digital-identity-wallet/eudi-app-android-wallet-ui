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

import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentedDocument
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import java.time.Instant

sealed interface TransactionLogData {

    val id: String
    val name: String
    val status: TransactionLog.Status
    val creationDate: Instant

    data class PresentationLog(
        override val id: String,
        override val name: String,
        override val status: TransactionLog.Status,
        override val creationDate: Instant,
        val relyingParty: TransactionLog.RelyingParty,
        val documents: List<PresentedDocument>,
    ) : TransactionLogData

    data class IssuanceLog(
        override val id: String,
        override val name: String,
        override val status: TransactionLog.Status,
        override val creationDate: Instant,
    ) : TransactionLogData

    data class SigningLog(
        override val id: String,
        override val name: String,
        override val status: TransactionLog.Status,
        override val creationDate: Instant,
    ) : TransactionLogData

    companion object {
        fun TransactionLogData.getTransactionTypeLabel(resourceProvider: ResourceProvider): String {
            return when (this) {
                is PresentationLog -> resourceProvider.getString(eu.europa.ec.resourceslogic.R.string.transactions_screen_filters_filter_by_transaction_type_presentation)
                is IssuanceLog -> resourceProvider.getString(eu.europa.ec.resourceslogic.R.string.transactions_screen_filters_filter_by_transaction_type_issuance)
                is SigningLog -> resourceProvider.getString(eu.europa.ec.resourceslogic.R.string.transactions_screen_filters_filter_by_transaction_type_signing)
            }
        }
    }
}