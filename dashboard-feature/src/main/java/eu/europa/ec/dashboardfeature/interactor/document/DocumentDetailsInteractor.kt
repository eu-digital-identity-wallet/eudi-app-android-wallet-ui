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
import eu.europa.ec.commonfeature.model.DocumentItemUi
import eu.europa.ec.commonfeature.model.DocumentStatusUi
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class DocumentDetailsInteractorPartialState {
    data class Success(val document: DocumentUi, val userName: String) :
        DocumentDetailsInteractorPartialState()

    data class Failure(val error: String) : DocumentDetailsInteractorPartialState()
}

interface DocumentDetailsInteractor {
    fun getDocument(documentId: String): Flow<DocumentDetailsInteractorPartialState>
}

class DocumentDetailsInteractorImpl(
    private val resourceProvider: ResourceProvider
) : DocumentDetailsInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getDocument(documentId: String): Flow<DocumentDetailsInteractorPartialState> =
        flow {
            delay(1_000L)
            emit(
                DocumentDetailsInteractorPartialState.Success(
                    document = DocumentUi(
                        documentId = "0",
                        documentType = DocumentTypeUi.DIGITAL_ID,
                        documentStatus = DocumentStatusUi.ACTIVE,
                        documentImage = "image",
                        documentItems = (1..15).map {
                            DocumentItemUi(title = "Title $it", value = "Value $it")
                        },
                    ),
                    userName = "Jane"
                )
            )
        }.safeAsync {
            DocumentDetailsInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }
}