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

package eu.europa.ec.eudi.wallet.ui

import android.app.Application
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.EudiWalletSDK

class WalletUIApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val config = EudiWalletConfig.Builder(this)
            .bleTransferMode(
                EudiWalletConfig.BLE_SERVER_PERIPHERAL_MODE,
                EudiWalletConfig.BLE_CLIENT_CENTRAL_MODE
            )
            .trustedReaderCertificates(R.raw.scytales_root_ca)
            .documentsStorageDir(noBackupFilesDir)
            .encryptDocumentsInStorage(true)
            .userAuthenticationRequired(false)
            .userAuthenticationTimeOut(30_000L)
            .useHardwareToStoreKeys(true)
            .openId4VpVerifierApiUri(BuildConfig.VERIFIER_API)
            .build()

        EudiWalletSDK.init(applicationContext, config)
    }
}