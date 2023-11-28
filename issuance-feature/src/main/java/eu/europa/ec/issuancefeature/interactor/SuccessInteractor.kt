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

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.sample.LoadSampleResult
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import java.util.Base64

sealed class SuccessPartialState {
    data object Success : SuccessPartialState()
    data class Failure(val error: String) : SuccessPartialState()
}

interface SuccessInteractor {
    fun addData(): Flow<SuccessPartialState>
}

class SuccessInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val eudiWallet: EudiWallet
) : SuccessInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun addData(): Flow<SuccessPartialState> = flow {
        val byteArray = Base64.getDecoder().decode(
            JSONObject(
                resourceProvider.getStringFromRaw(R.raw.sample_data)
            ).getString("Data")
        )

        val result = eudiWallet.loadSampleData(byteArray)

        emit(
            when (result) {
                is LoadSampleResult.Error -> {
                    SuccessPartialState.Failure(
                        error = result.message
                    )
                }

                LoadSampleResult.Success -> {
                    SuccessPartialState.Success
                }
            }
        )
    }.safeAsync {
        SuccessPartialState.Failure(
            error = it.message ?: genericErrorMsg
        )
    }
}