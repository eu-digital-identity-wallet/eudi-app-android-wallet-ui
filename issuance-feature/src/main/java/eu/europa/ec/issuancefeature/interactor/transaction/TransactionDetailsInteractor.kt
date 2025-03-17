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

package eu.europa.ec.issuancefeature.interactor.transaction

import eu.europa.ec.commonfeature.ui.transaction_details.domain.TransactionClaimItem
import eu.europa.ec.commonfeature.ui.transaction_details.domain.TransactionDetailsDomain
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

sealed class TransactionDetailsInteractorPartialState {
    data class Success(
        val detailsTitle: String,
        val transactionDetailsDomain: TransactionDetailsDomain,
    ) : TransactionDetailsInteractorPartialState()

    data class Failure(val error: String) : TransactionDetailsInteractorPartialState()
}

sealed class TransactionDetailsInteractorRequestDataDeletionPartialState {
    data object Success : TransactionDetailsInteractorRequestDataDeletionPartialState()
    data class Failure(
        val errorMessage: String
    ) : TransactionDetailsInteractorRequestDataDeletionPartialState()
}

sealed class TransactionDetailsInteractorReportSuspiciousTransactionPartialState {
    data object Success : TransactionDetailsInteractorReportSuspiciousTransactionPartialState()
    data class Failure(
        val errorMessage: String
    ) : TransactionDetailsInteractorReportSuspiciousTransactionPartialState()
}

interface TransactionDetailsInteractor {
    fun getTransactionDetails(
        transactionId: String
    ): Flow<TransactionDetailsInteractorPartialState>

    fun requestDataDeletion(transactionId: String): Flow<TransactionDetailsInteractorRequestDataDeletionPartialState>
    fun reportSuspiciousTransaction(transactionId: String): Flow<TransactionDetailsInteractorReportSuspiciousTransactionPartialState>
}

class TransactionDetailsInteractorImpl(
    private val resourceProvider: ResourceProvider
) : TransactionDetailsInteractor {
    override fun getTransactionDetails(transactionId: String): Flow<TransactionDetailsInteractorPartialState> {
        return flowOf(
            TransactionDetailsInteractorPartialState.Success(
                detailsTitle = resourceProvider.getString(R.string.transaction_details_screen_title),
                transactionDetailsDomain = TransactionDetailsDomain(
                    transactionName = "A transaction name",
                    transactionId = transactionId,
                    sharedDataClaimItems = listOf(
                        TransactionClaimItem(
                            transactionId = "0",
                            value = "John",
                            readableName = "given_name"
                        ),
                        TransactionClaimItem(
                            transactionId = "1",
                            value = "Doe",
                            readableName = "family_name"
                        ),
                    ),
                    signedDataClaimItems = listOf(
                        TransactionClaimItem(
                            transactionId = "0",
                            value = "John",
                            readableName = "given_name"
                        ),
                        TransactionClaimItem(
                            transactionId = "1",
                            value = "Doe",
                            readableName = "family_name"
                        )
                    )
                )
            )
        )
    }

    override fun requestDataDeletion(transactionId: String): Flow<TransactionDetailsInteractorRequestDataDeletionPartialState> {
        return flowOf(
            TransactionDetailsInteractorRequestDataDeletionPartialState.Success
        )
    }

    override fun reportSuspiciousTransaction(transactionId: String): Flow<TransactionDetailsInteractorReportSuspiciousTransactionPartialState> {
        return flowOf(
            TransactionDetailsInteractorReportSuspiciousTransactionPartialState.Success
        )
    }
}