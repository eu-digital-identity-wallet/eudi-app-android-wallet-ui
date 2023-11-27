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
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.sample.LoadSampleResult
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.AppIcons
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import java.util.Base64

sealed class AddDocumentInteractorPartialState {
    data class Success(val options: List<DocumentOptionItemUi>) :
        AddDocumentInteractorPartialState()

    data class Failure(val error: String) : AddDocumentInteractorPartialState()
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