/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.europa.ec.eudi.wallet.ui.util

import androidx.activity.ComponentActivity
import eu.europa.ec.eudi.wallet.ui.util.EuPidIssuance.Result.Failure
import eu.europa.ec.eudi.wallet.ui.util.EuPidIssuance.Result.Success
import eu.europa.ec.eudi.web.lightIssuing.EudiPidIssuer
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.AddDocumentResult
import eu.europa.ec.eudi.wallet.document.Constants
import eu.europa.ec.eudi.wallet.document.CreateIssuanceRequestResult
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.DocumentManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Class for issuing EU PID documents. This class implements the bridge between [DocumentManager]
 * and the [EudiPidIssuer] in order to issue the eu.europa.ec.eudiw.pid.1 document.
 *
 * @constructor Create empty Eu pid issuance
 */
object EuPidIssuance {

    private const val docType = Constants.EU_PID_DOCTYPE

    /**
     * Method to issue the document.
     *
     * @param activity required to launch the [EudiPidIssuer] activity
     * @param country the country to issue the document for
     * @return
     */
    suspend fun issueDocument(activity: ComponentActivity, country: EudiPidIssuer.Country): Result {
        val hardwareBacked = EudiWallet.config.useHardwareToStoreKeys
        return when (val r1 = EudiWallet.createIssuanceRequest(docType, hardwareBacked)) {
            is CreateIssuanceRequestResult.Failure -> Failure(r1.throwable)
            is CreateIssuanceRequestResult.Success -> {
                val request = r1.issuanceRequest
                val certificates = listOf(request.certificateNeedAuth)
                val r2 = suspendCoroutine { cont ->
                    EudiPidIssuer.issueDocument(activity, country, certificates) {
                        cont.resume(
                            it
                        )
                    }
                }
                when (r2) {
                    is EudiPidIssuer.Result.Failure -> Failure(r2.throwable)
                    is EudiPidIssuer.Result.Success -> {
                        val bytes = r2.documentBytes
                        when (val r3 = EudiWallet.addDocument(request, bytes)) {
                            is AddDocumentResult.Failure -> Failure(r3.throwable)
                            is AddDocumentResult.Success -> Success(r3.documentId)
                        }
                    }
                }
            }
        }
    }

    /**
     * EuPid issuance result. Can be either [Success] or [Failure]
     *
     * @constructor Create empty Result
     */
    sealed interface Result {
        /**
         * Issuance success result. Contains the documentId.
         * DocumentId can be then used to retrieve the document from the
         * [DocumentManager::getDocumentById] method
         *
         * @property documentId
         * @constructor Create empty Success
         */
        class Success(val documentId: DocumentId) : Result

        /**
         * Issuance failure result. Contains the throwable that caused the failure
         *
         * @property throwable
         * @constructor Create empty Failure
         */
        data class Failure(val throwable: Throwable) : Result
    }
}