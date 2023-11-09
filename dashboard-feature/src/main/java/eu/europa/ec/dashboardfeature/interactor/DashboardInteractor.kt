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

package eu.europa.ec.dashboardfeature.interactor

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.commonfeature.model.DocumentStatusUi
import eu.europa.ec.commonfeature.model.DocumentTypeUi
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class DashboardInteractorPartialState {
    data class Success(val documents: List<DocumentUi>) : DashboardInteractorPartialState()
    data class Failure(val error: String) : DashboardInteractorPartialState()
}

interface DashboardInteractor {
    fun getDocuments(): Flow<DashboardInteractorPartialState>
    fun getUserName(): String
}

class DashboardInteractorImpl(
    private val resourceProvider: ResourceProvider
) : DashboardInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getDocuments(): Flow<DashboardInteractorPartialState> = flow {
        delay(1_000L)
        emit(
            DashboardInteractorPartialState.Success(
                documents = getFakeDocuments()
            )
        )
    }.safeAsync {
        DashboardInteractorPartialState.Failure(
            error = it.localizedMessage ?: genericErrorMsg
        )
    }

    override fun getUserName(): String {
        return "Jane"
    }

    private fun getFakeDocuments(): List<DocumentUi> {
        return listOf(
            DocumentUi(
                documentId = 0,
                documentType = DocumentTypeUi.DIGITAL_ID,
                documentStatus = DocumentStatusUi.ACTIVE,
                documentImage = "image1",
                documentItems = emptyList()
            ),
            DocumentUi(
                documentId = 1,
                documentType = DocumentTypeUi.DRIVING_LICENCE,
                documentStatus = DocumentStatusUi.ACTIVE,
                documentImage = "image2",
                documentItems = emptyList()
            ),
            DocumentUi(
                documentId = 2,
                documentType = DocumentTypeUi.OTHER,
                documentStatus = DocumentStatusUi.ACTIVE,
                documentImage = "image3",
                documentItems = emptyList()
            ),
            DocumentUi(
                documentId = 3,
                documentType = DocumentTypeUi.DIGITAL_ID,
                documentStatus = DocumentStatusUi.INACTIVE,
                documentImage = "image4",
                documentItems = emptyList()
            ),
            DocumentUi(
                documentId = 4,
                documentType = DocumentTypeUi.DIGITAL_ID,
                documentStatus = DocumentStatusUi.ACTIVE,
                documentImage = "image1",
                documentItems = emptyList()
            ),
            DocumentUi(
                documentId = 5,
                documentType = DocumentTypeUi.DRIVING_LICENCE,
                documentStatus = DocumentStatusUi.ACTIVE,
                documentImage = "image2",
                documentItems = emptyList()
            ),
            DocumentUi(
                documentId = 6,
                documentType = DocumentTypeUi.OTHER,
                documentStatus = DocumentStatusUi.ACTIVE,
                documentImage = "image3",
                documentItems = emptyList()
            ),
            DocumentUi(
                documentId = 7,
                documentType = DocumentTypeUi.DIGITAL_ID,
                documentStatus = DocumentStatusUi.INACTIVE,
                documentImage = "image4",
                documentItems = emptyList()
            ),
            DocumentUi(
                documentId = 8,
                documentType = DocumentTypeUi.DIGITAL_ID,
                documentStatus = DocumentStatusUi.ACTIVE,
                documentImage = "image1",
                documentItems = emptyList()
            ),
            DocumentUi(
                documentId = 9,
                documentType = DocumentTypeUi.DRIVING_LICENCE,
                documentStatus = DocumentStatusUi.ACTIVE,
                documentImage = "image2",
                documentItems = emptyList()
            ),
            DocumentUi(
                documentId = 10,
                documentType = DocumentTypeUi.OTHER,
                documentStatus = DocumentStatusUi.ACTIVE,
                documentImage = "image3",
                documentItems = emptyList()
            ),
            DocumentUi(
                documentId = 11,
                documentType = DocumentTypeUi.DIGITAL_ID,
                documentStatus = DocumentStatusUi.INACTIVE,
                documentImage = "image4",
                documentItems = emptyList()
            ),
            DocumentUi(
                documentId = 12,
                documentType = DocumentTypeUi.DIGITAL_ID,
                documentStatus = DocumentStatusUi.ACTIVE,
                documentImage = "image1",
                documentItems = emptyList()
            ),
            DocumentUi(
                documentId = 13,
                documentType = DocumentTypeUi.DRIVING_LICENCE,
                documentStatus = DocumentStatusUi.ACTIVE,
                documentImage = "image2",
                documentItems = emptyList()
            ),
            DocumentUi(
                documentId = 14,
                documentType = DocumentTypeUi.OTHER,
                documentStatus = DocumentStatusUi.ACTIVE,
                documentImage = "image3",
                documentItems = emptyList()
            ),
            DocumentUi(
                documentId = 15,
                documentType = DocumentTypeUi.DIGITAL_ID,
                documentStatus = DocumentStatusUi.INACTIVE,
                documentImage = "image4",
                documentItems = emptyList()
            )
        )
    }
}