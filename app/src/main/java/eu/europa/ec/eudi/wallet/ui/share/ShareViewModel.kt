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

package eu.europa.ec.eudi.wallet.ui.share

import android.app.Application
import android.content.Intent
import android.view.View
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.wallet.EudiWalletSDK

class ShareViewModel(app: Application) : AndroidViewModel(app) {


    private val _events = MutableLiveData<TransferEvent>()
    val events: LiveData<TransferEvent>
        get() = _events
    private val transferEventListener = TransferEvent.Listener { event ->
        _events.value = event
    }

    init {
        EudiWalletSDK.addTransferEventListener(transferEventListener)
    }

    var deviceEngagementQr = ObservableField<View>()
    var message = ObservableField<String>()
    var isLoading = ObservableField(true)

    fun startQrEngagement() {
        EudiWalletSDK.startQrEngagement()
    }

    fun showQrCode(qrCode: View) {
        isLoading.set(false)
        deviceEngagementQr.set(qrCode)
    }

    fun startEngagementToApp(intent: Intent) {
        EudiWalletSDK.startEngagementToApp(intent)
    }

    fun cancelPresentation() {
        EudiWalletSDK.stopPresentation()
        message.set("")
    }
}