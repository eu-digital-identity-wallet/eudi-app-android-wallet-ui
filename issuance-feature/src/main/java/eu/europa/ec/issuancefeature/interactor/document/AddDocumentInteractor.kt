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

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.commonfeature.model.DocumentOptionItemUi
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class AddDocumentInteractorPartialState {
    data class Success(val options: List<DocumentOptionItemUi>) :
        AddDocumentInteractorPartialState()

    data class Failure(val error: String) : AddDocumentInteractorPartialState()
}

interface AddDocumentInteractor {
    fun getAddDocumentOption(): Flow<AddDocumentInteractorPartialState>
}

class AddDocumentInteractorImpl(
    private val resourceProvider: ResourceProvider
) : AddDocumentInteractor {
    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getAddDocumentOption(): Flow<AddDocumentInteractorPartialState> = flow {
        delay(400L)
        emit(
            AddDocumentInteractorPartialState.Success(
                options = getFakeAddDocumentOptions()
            )
        )
    }.safeAsync {
        AddDocumentInteractorPartialState.Failure(
            error = it.localizedMessage ?: genericErrorMsg
        )
    }

    private fun getFakeAddDocumentOptions(): List<DocumentOptionItemUi> {
        return listOf(
            DocumentOptionItemUi(
                text = resourceProvider.getString(R.string.add_id),
                icon = AppIcons.Id,
                type = DocumentTypeUi.DIGITAL_ID,
                issuanceUrl = "www.gov.gr"
            ),
            DocumentOptionItemUi(
                text = resourceProvider.getString(R.string.add_driving_license),
                icon = AppIcons.Id,
                type = DocumentTypeUi.DRIVING_LICENSE,
                issuanceUrl = "www.gov-automotive.gr"
            )
        )
    }
}