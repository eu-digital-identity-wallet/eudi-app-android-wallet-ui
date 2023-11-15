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

package eu.europa.ec.commonfeature.corewrapper

import eu.europa.ec.eudi.iso18013.transfer.TransferEvent

class EUDIWListenerWrapper(
    private val onConnected: () -> Unit = {},
    private val onConnecting: () -> Unit = {},
    private val onDisconnected: () -> Unit = {},
    private val onError: (String) -> Unit = {},
    private val onQrEngagementReady: (String) -> Unit = {},
    private val onRequestReceived: () -> Unit = {},
    private val onResponseSent: () -> Unit = {},
) : TransferEvent.Listener {
    override fun onTransferEvent(event: TransferEvent) {
        when (event) {
            is TransferEvent.Connected -> onConnected()
            is TransferEvent.Connecting -> onConnecting()
            is TransferEvent.Disconnected -> onDisconnected()
            is TransferEvent.Error -> onError(event.error.message ?: "")
            is TransferEvent.QrEngagementReady -> onQrEngagementReady(event.qrCode.content)
            is TransferEvent.RequestReceived -> onRequestReceived()
            is TransferEvent.ResponseSent -> onResponseSent()
        }
    }
}