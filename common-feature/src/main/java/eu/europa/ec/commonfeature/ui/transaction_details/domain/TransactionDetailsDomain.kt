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

package eu.europa.ec.commonfeature.ui.transaction_details.domain

import eu.europa.ec.eudi.wallet.document.ElementIdentifier

data class TransactionDetailsDomain(
    val transactionName: String,
    val transactionId: String = "id",
    val transactionIdentifier: String? = null,
    val sharedDataClaimItems: List<TransactionClaimItem>,
    val signedDataClaimItems: List<TransactionClaimItem>
)

data class TransactionClaimItem(
    val transactionId: String,
    val elementIdentifier: ElementIdentifier? = "randomId",
    val value: String = "testValue",
    val readableName: String = "test_readable_name",
    val isAvailable: Boolean = true
)