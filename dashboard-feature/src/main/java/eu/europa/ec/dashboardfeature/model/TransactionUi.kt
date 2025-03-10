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
import eu.europa.ec.uilogic.component.ListItemData

data class TransactionUi(
    val uiData: ListItemData,
    val uiStatus: TransactionUiStatus,
    val transactionCategory: TransactionCategory,
) : FilterableItemPayload

enum class TransactionUiStatus {
    Completed, Failed
}

internal fun String.toTransactionUiStatus(completedStatusString: String): TransactionUiStatus =
    when {
        equals(completedStatusString, ignoreCase = true) -> TransactionUiStatus.Completed
        else -> TransactionUiStatus.Failed
    }

// TODO should be replaced with actual transaction data
sealed interface Transaction {
    val id: String
    val name: String
    val status: String
    val creationDate: String
}

data class DocumentSigningTransaction(
    override val id: String,
    override val name: String,
    override val status: String,
    override val creationDate: String
) : Transaction

data class AttestationPresentationTransaction(
    override val id: String,
    override val name: String,
    override val status: String,
    override val creationDate: String,
    val issuerName: String,
    val relyingPartyName: String,
    val attestationType: String
) : Transaction

data class OtherTransaction(
    override val id: String,
    override val name: String,
    override val status: String,
    override val creationDate: String
) : Transaction