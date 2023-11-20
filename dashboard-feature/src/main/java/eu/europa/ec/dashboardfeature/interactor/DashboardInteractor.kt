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
import eu.europa.ec.commonfeature.model.DocumentUi
import eu.europa.ec.commonfeature.model.toDocumentTypeUi
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.Constants
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.nameSpacedDataJSONObject
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONException
import org.json.JSONObject

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
    private val eudiWallet: EudiWallet,
) : DashboardInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getDocuments(): Flow<DashboardInteractorPartialState> = flow {
        val documents = eudiWallet.getDocuments()
        val (documentsUi, name) = mapToUi(documents)
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

    private fun mapToUi(documents: List<Document>): Pair<List<DocumentUi>, String> {
        val nameKey = "given_name"
        var name = ""
        val documentsUi = documents.map {
            extractJsonIDDocument(it)?.let { jsonDocument ->
                if (name.isBlank()
                    && jsonDocument.has(nameKey)
                ) {
                    name = jsonDocument.getString(nameKey)
                }
            }

            DocumentUi(
                documentId = it.id,
                documentType = it.docType.toDocumentTypeUi(),
                documentImage = "",
                documentStatus = DocumentStatusUi.ACTIVE,
                documentItems = emptyList()
            )
        }

        return Pair(documentsUi, name)
    }

    private fun extractJsonIDDocument(document: Document): JSONObject? =
        try {
            document.nameSpacedDataJSONObject.getJSONObject(Constants.EU_PID_DOCTYPE)
        } catch (ex: JSONException) {
            null
        }
}