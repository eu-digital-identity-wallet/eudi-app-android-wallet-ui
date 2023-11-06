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
@file:JvmMultifileClass

package eu.europa.ec.eudi.wallet.ui.transfer

import android.app.Application
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import eu.europa.ec.eudi.wallet.ui.selectivedisclosure.SelectedDocumentCollection
import eu.europa.ec.eudi.wallet.ui.selectivedisclosure.UserAuthStatus
import eu.europa.ec.eudi.wallet.ui.util.log
import eu.europa.ec.eudi.iso18013.transfer.DisclosedDocuments
import eu.europa.ec.eudi.iso18013.transfer.DocItem
import eu.europa.ec.eudi.iso18013.transfer.RequestDocument
import eu.europa.ec.eudi.iso18013.transfer.ResponseResult
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.wallet.EudiWalletSDK
import kotlinx.coroutines.launch

class TransferViewModel(val app: Application) : AndroidViewModel(app) {

    private val _events = MutableLiveData<TransferEvent>()
    val events: LiveData<TransferEvent>
        get() = _events
    private val transferEventListener = TransferEvent.Listener { event ->
        _events.value = event

    }

    init {
        EudiWalletSDK.addTransferEventListener(transferEventListener)
    }

    private var selectedDocuments = SelectedDocumentCollection()

    private val mutableUserAuthStatus = MutableLiveData<UserAuthStatus?>()
    val userAuthStatus: LiveData<UserAuthStatus?> = mutableUserAuthStatus

    var isLoading = ObservableBoolean(true)

    private val openId4VpManager
        get() = EudiWalletSDK.openId4vpManager.apply {
            removeAllTransferEventListeners() // clear previous listeners
            addTransferEventListener(transferEventListener) // add only current listener
        }

    private var openid4VPMode: Boolean = false

    fun handleOpenId4Vp(openId4VpURI: String) {
        openid4VPMode = true
        viewModelScope.launch {
            openId4VpManager.resolveRequestUri(openId4VpURI)
        }
    }

    fun updateUserAuthStatus(status: UserAuthStatus) {
        mutableUserAuthStatus.value = status
    }

    fun onUserAuthConsumed() {
        mutableUserAuthStatus.value = null
    }

    fun addDocumentForSelection(requestDocumentData: RequestDocument) {
        selectedDocuments.addDocument(requestDocumentData)
    }

    fun toggleDocItem(credentialName: String, docItem: DocItem) {
        selectedDocuments.toggleDocItem(credentialName, docItem)
    }

    /**
     * Creates and send the response
     *
     * @return true if success; false if userAuth is required
     */
    fun sendDocuments(): SendDocumentsResult {
        return when (val result =
            EudiWalletSDK.createResponse(DisclosedDocuments(selectedDocuments.collect()))) {
            is ResponseResult.Response -> {
                if (openid4VPMode) { // send openid4vp response
                    viewModelScope.launch {
                        openId4VpManager.sendResponse(result.bytes)
                    }
                } else {
                    EudiWalletSDK.sendResponse(result.bytes)
                }
                cleanUp()
                SendDocumentsResult.Success
            }

            is ResponseResult.UserAuthRequired ->
                SendDocumentsResult.UserAuthRequired(result.cryptoObject)

            is ResponseResult.Failure -> {
                log("sendDocuments", result.throwable)
                cleanUp()
                SendDocumentsResult.Failure(result.throwable)
            }
        }
    }

    fun cancelPresentation() {
        EudiWalletSDK.stopPresentation()
        openid4VPMode = false
        openId4VpManager.close()
        cleanUp()
    }

    private fun cleanUp() {
        isLoading.set(false)
        selectedDocuments.clear()
    }
}

sealed interface SendDocumentsResult {
    object Success : SendDocumentsResult
    data class UserAuthRequired(val cryptoObject: CryptoObject?) : SendDocumentsResult

    data class Failure(val throwable: Throwable) : SendDocumentsResult
}