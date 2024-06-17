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

package eu.europa.ec.issuancefeature.interactor

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.commonfeature.model.toUiName
import eu.europa.ec.commonfeature.util.extractFullNameFromDocumentOrEmpty
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class SuccessFetchDocumentByIdPartialState {
    data class Success(
        val document: Document,
        val documentName: String,
        val fullName: String
    ) : SuccessFetchDocumentByIdPartialState()

    data class Failure(val error: String) : SuccessFetchDocumentByIdPartialState()
}

interface SuccessInteractor {
    fun fetchDocumentById(id: String): Flow<SuccessFetchDocumentByIdPartialState>
}

class SuccessInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController
) : SuccessInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun fetchDocumentById(id: String): Flow<SuccessFetchDocumentByIdPartialState> = flow {
        val document = walletCoreDocumentsController.getDocumentById(id = id)
        document?.let {
            emit(
                SuccessFetchDocumentByIdPartialState.Success(
                    document = it,
                    documentName = it.toUiName(resourceProvider),
                    fullName = extractFullNameFromDocumentOrEmpty(it)
                )
            )
        } ?: emit(SuccessFetchDocumentByIdPartialState.Failure(genericErrorMsg))
    }.safeAsync {
        SuccessFetchDocumentByIdPartialState.Failure(
            error = it.localizedMessage ?: genericErrorMsg
        )
    }
}