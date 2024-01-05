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

import eu.europa.ec.businesslogic.controller.walletcore.IssuanceMethod
import eu.europa.ec.businesslogic.controller.walletcore.IssueDocumentPartialState
import eu.europa.ec.businesslogic.controller.walletcore.WalletCoreDocumentsController
import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.commonfeature.model.DocumentOptionItemUi
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class AddDocumentInteractorPartialState {
    data class Success(val options: List<DocumentOptionItemUi>) :
        AddDocumentInteractorPartialState()

    data class Failure(val error: String) : AddDocumentInteractorPartialState()
}

interface AddDocumentInteractor {
    fun getAddDocumentOption(): Flow<AddDocumentInteractorPartialState>

    fun issueDocument(
        issuanceMethod: IssuanceMethod,
        documentType: String
    ): Flow<IssueDocumentPartialState>
}

class AddDocumentInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val resourceProvider: ResourceProvider,
) : AddDocumentInteractor {
    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getAddDocumentOption(): Flow<AddDocumentInteractorPartialState> = flow {
        emit(
            AddDocumentInteractorPartialState.Success(
                options = listOf(
                    DocumentOptionItemUi(
                        text = DocumentTypeUi.DIGITAL_ID.uiName,
                        icon = AppIcons.Id,
                        type = DocumentTypeUi.DIGITAL_ID,
                        available = true
                    ),
                    DocumentOptionItemUi(
                        text = DocumentTypeUi.DRIVING_LICENSE.uiName,
                        icon = AppIcons.Id,
                        type = DocumentTypeUi.DRIVING_LICENSE,
                        available = false
                    )
                )
            )
        )
    }.safeAsync {
        AddDocumentInteractorPartialState.Failure(
            error = it.localizedMessage ?: genericErrorMsg
        )
    }

    override fun issueDocument(
        issuanceMethod: IssuanceMethod,
        documentType: String
    ): Flow<IssueDocumentPartialState> =
        walletCoreDocumentsController.issueDocument(
            issuanceMethod = issuanceMethod,
            documentType = documentType
        )
}