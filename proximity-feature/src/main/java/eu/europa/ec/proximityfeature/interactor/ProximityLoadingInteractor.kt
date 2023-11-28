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

package eu.europa.ec.proximityfeature.interactor

import eu.europa.ec.commonfeature.interactor.EudiWalletInteractor
import eu.europa.ec.commonfeature.interactor.EudiWalletProximityPartialState
import kotlinx.coroutines.flow.Flow

interface ProximityLoadingInteractor {
    val verifierName: String?
    fun stopPresentation()
    fun observeResponse(): Flow<EudiWalletProximityPartialState>
}

class ProximityLoadingInteractorImpl(
    private val eudiWalletInteractor: EudiWalletInteractor
) : ProximityLoadingInteractor {

    override val verifierName: String? = eudiWalletInteractor.verifierName

    override fun observeResponse(): Flow<EudiWalletProximityPartialState> =
        eudiWalletInteractor.observeSentDocumentsRequest()

    override fun stopPresentation() {
        eudiWalletInteractor.stopPresentation()
    }
}