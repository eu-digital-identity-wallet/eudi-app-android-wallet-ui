/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.dashboardfeature.interactor.document

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
                options = listOf(
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
            )
        )
    }.safeAsync {
        AddDocumentInteractorPartialState.Failure(
            error = it.localizedMessage ?: genericErrorMsg
        )
    }
}