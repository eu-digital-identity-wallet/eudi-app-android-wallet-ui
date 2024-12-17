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
import eu.europa.ec.commonfeature.util.extractFullNameFromDocumentOrEmpty
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class SuccessFetchDocumentByIdPartialState {
    data class Success(
        val document: IssuedDocument,
        val documentName: String,
        val fullName: String
    ) : SuccessFetchDocumentByIdPartialState()

    data class Failure(val error: String) : SuccessFetchDocumentByIdPartialState()
}

interface SuccessInteractor {
    fun fetchDocumentById(documentId: DocumentId): Flow<SuccessFetchDocumentByIdPartialState>
}

class SuccessInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController
) : SuccessInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun fetchDocumentById(documentId: DocumentId): Flow<SuccessFetchDocumentByIdPartialState> =
        flow {
            val document = walletCoreDocumentsController.getDocumentById(documentId = documentId)
                    as? IssuedDocument
            document?.let { issuedDocument ->
                emit(
                    SuccessFetchDocumentByIdPartialState.Success(
                        document = issuedDocument,
                        documentName = issuedDocument.name,
                        fullName = extractFullNameFromDocumentOrEmpty(issuedDocument)
                    )
                )
            } ?: emit(SuccessFetchDocumentByIdPartialState.Failure(genericErrorMsg))
        }.safeAsync {
            SuccessFetchDocumentByIdPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }
}