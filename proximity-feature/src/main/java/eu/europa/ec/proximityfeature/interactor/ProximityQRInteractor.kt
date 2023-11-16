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

package eu.europa.ec.proximityfeature.interactor

import eu.europa.ec.businesslogic.extension.safeAsync
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class ProximityQRInteractorPartialState {
    data class Success(val qRCode: String) : ProximityQRInteractorPartialState()
    data class Failure(val error: String) : ProximityQRInteractorPartialState()
}

interface ProximityQRInteractor {
    fun generateQRCode(): Flow<ProximityQRInteractorPartialState>
}

class ProximityQRInteractorImpl(
    private val resourceProvider: ResourceProvider,
) : ProximityQRInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun generateQRCode(): Flow<ProximityQRInteractorPartialState> = flow {
        emit(
            ProximityQRInteractorPartialState.Success(
                qRCode = "some text"
            )
        )
    }.safeAsync {
        ProximityQRInteractorPartialState.Failure(
            error = it.localizedMessage ?: genericErrorMsg
        )
    }
}