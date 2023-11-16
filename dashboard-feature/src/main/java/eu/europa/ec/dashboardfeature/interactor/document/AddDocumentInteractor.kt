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
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.sample.LoadSampleResult
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import java.util.Base64

sealed class AddDocumentOptions {
    data class Success(val options: List<DocumentOptionItemUi>) :
        AddDocumentOptions()

    data class Failure(val error: String) : AddDocumentOptions()
}

sealed class AddDocumentLoadData {
    data object Success :
        AddDocumentLoadData()

    data class Failure(val error: String) : AddDocumentLoadData()
}

interface AddDocumentInteractor {
    fun getAddDocumentOption(): Flow<AddDocumentOptions>
    fun addSampleData(): Flow<AddDocumentLoadData>
}

class AddDocumentInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val eudiWallet: EudiWallet,
) : AddDocumentInteractor {
    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getAddDocumentOption(): Flow<AddDocumentOptions> = flow {
        emit(
            AddDocumentOptions.Success(
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
        AddDocumentOptions.Failure(
            error = it.localizedMessage ?: genericErrorMsg
        )
    }

    override fun addSampleData(): Flow<AddDocumentLoadData> = flow {
        val byteArray = Base64.getDecoder().decode(
            JSONObject(
                resourceProvider.getStringFromRaw(R.raw.sample_data)
            ).getString("Data")
        )

        val result = eudiWallet.loadSampleData(byteArray)

        emit(
            when (result) {
                is LoadSampleResult.Error -> {
                    AddDocumentLoadData.Failure(
                        error = result.message
                    )
                }

                LoadSampleResult.Success -> {
                    AddDocumentLoadData.Success
                }
            }
        )
    }.safeAsync {
        AddDocumentLoadData.Failure(
            error = it.localizedMessage ?: genericErrorMsg
        )
    }
}