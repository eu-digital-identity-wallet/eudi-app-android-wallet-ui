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

package eu.europa.ec.dashboardfeature.interactor

import eu.europa.ec.businesslogic.controller.walletcore.WalletCoreDocumentsController
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.commonfeature.model.DocumentStatusUi
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.model.toDocumentTypeUi
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class DashboardInteractorPartialState {
    data class Success(val documents: List<DocumentUi>, val name: String) :
        DashboardInteractorPartialState()

    data class Failure(val error: String) : DashboardInteractorPartialState()
}

interface DashboardInteractor {
    fun getDocuments(): Flow<DashboardInteractorPartialState>
}

class DashboardInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
) : DashboardInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getDocuments(): Flow<DashboardInteractorPartialState> = flow {
        val documents = walletCoreDocumentsController.getSampleDocuments()
        val (documentsUi, name) = documents.map {
            DocumentUi(
                documentId = it.id,
                documentName = it.name,
                documentType = it.docType.toDocumentTypeUi(),
                documentImage = "",
                documentStatus = DocumentStatusUi.ACTIVE,
                documentDetails = emptyList()
            )
        } to "Jane Doe"
        emit(
            DashboardInteractorPartialState.Success(
                documents = documentsUi,
                name = name
            )
        )
    }.safeAsync {
        DashboardInteractorPartialState.Failure(
            error = it.localizedMessage ?: genericErrorMsg
        )
    }
}