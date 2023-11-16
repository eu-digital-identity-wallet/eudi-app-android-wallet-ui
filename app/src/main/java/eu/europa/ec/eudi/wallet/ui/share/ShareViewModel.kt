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

package eu.europa.ec.eudi.wallet.ui.share

import android.app.Application
import android.content.Intent
import android.view.View
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.wallet.EudiWallet

class ShareViewModel(app: Application) : AndroidViewModel(app) {


    private val _events = MutableLiveData<TransferEvent>()
    val events: LiveData<TransferEvent>
        get() = _events
    private val transferEventListener = TransferEvent.Listener { event ->
        _events.value = event
    }

    init {
        EudiWallet.addTransferEventListener(transferEventListener)
    }

    var deviceEngagementQr = ObservableField<View>()
    var message = ObservableField<String>()
    var isLoading = ObservableField(true)

    fun startQrEngagement() {
        EudiWallet.startQrEngagement()
    }

    fun showQrCode(qrCode: View) {
        isLoading.set(false)
        deviceEngagementQr.set(qrCode)
    }

    fun startEngagementToApp(intent: Intent) {
        EudiWallet.startEngagementToApp(intent)
    }

    fun cancelPresentation() {
        EudiWallet.stopPresentation()
        message.set("")
    }
}