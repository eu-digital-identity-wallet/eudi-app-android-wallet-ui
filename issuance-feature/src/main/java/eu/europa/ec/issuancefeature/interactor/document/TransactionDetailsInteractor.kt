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

package eu.europa.ec.issuancefeature.interactor.document

import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

sealed class TransactionDetailsInteractorPartialState {
    data class Success(
        val detailsTitle: String
    ) : TransactionDetailsInteractorPartialState()

    data class Failure(val error: String) : TransactionDetailsInteractorPartialState()
}

interface TransactionDetailsInteractor {
    fun getTransactionDetails(
        transactionId: String
    ): Flow<TransactionDetailsInteractorPartialState>
}

class TransactionDetailsInteractorImpl(
    private val resourceProvider: ResourceProvider
) : TransactionDetailsInteractor {
    override fun getTransactionDetails(transactionId: String): Flow<TransactionDetailsInteractorPartialState> {
        return flowOf(
            TransactionDetailsInteractorPartialState.Success(
                detailsTitle = "Transaction information"
            )
        )
    }
}