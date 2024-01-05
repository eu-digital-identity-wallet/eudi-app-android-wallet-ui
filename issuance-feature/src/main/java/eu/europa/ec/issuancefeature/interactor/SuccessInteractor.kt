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

import eu.europa.ec.businesslogic.controller.walletcore.LoadSampleDataPartialState
import eu.europa.ec.businesslogic.controller.walletcore.WalletCoreDocumentsController
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.commonfeature.util.extractFullNameFromDocumentOrEmpty
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.util.Base64

sealed class SuccessPartialState {
    data object Success : SuccessPartialState()
    data class Failure(val error: String) : SuccessPartialState()
}

sealed class SuccessFetchDocumentByIdPartialState {
    data class Success(
        val document: Document,
        val fullName: String
    ) : SuccessFetchDocumentByIdPartialState()

    data class Failure(val error: String) : SuccessFetchDocumentByIdPartialState()
}

interface SuccessInteractor {
    fun addSampleData(): Flow<SuccessPartialState>

    fun fetchDocumentById(id: String): Flow<SuccessFetchDocumentByIdPartialState>
}

class SuccessInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController
) : SuccessInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun addSampleData(): Flow<SuccessPartialState> = flow {
        // Iff user just added their first document, i.e. if it is their first time opening the app,
        // then also add the sample documents.
        if (walletCoreDocumentsController.getAllDocuments().size == 1) {
            val byteArray = Base64.getDecoder().decode(
                JSONObject(
                    resourceProvider.getStringFromRaw(R.raw.sample_data)
                ).getString("Data")
            )

            walletCoreDocumentsController.loadSampleData(byteArray).map {
                when (it) {
                    is LoadSampleDataPartialState.Failure -> SuccessPartialState.Failure(it.error)
                    is LoadSampleDataPartialState.Success -> SuccessPartialState.Success
                }
            }.collect {
                emit(it)
            }
        } else {
            emit(SuccessPartialState.Success)
            return@flow
        }
    }.safeAsync {
        SuccessPartialState.Failure(it.localizedMessage ?: genericErrorMsg)
    }

    override fun fetchDocumentById(id: String): Flow<SuccessFetchDocumentByIdPartialState> = flow {
        val document = walletCoreDocumentsController.getDocumentById(id = id)
        document?.let {
            emit(
                SuccessFetchDocumentByIdPartialState.Success(
                    document = it,
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