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

package eu.europa.ec.corelogic.config.certificate

import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CertificateHandler(private val context: Context) {

    // Main function to handle incoming URLs
    fun handleNewCertificateUrl(intent: Intent?) {
        if (intent?.action.equals("android.intent.action.SEND") == true) {
            intent?.extras?.getString("android.intent.extra.TEXT")?.let { url ->
                if (url.contains("pem", ignoreCase = false) ||
                    url.contains("cer", ignoreCase = false)) {
                    processCertificateUrl(url)
                }
            }
        }
    }

    private fun processCertificateUrl(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Download and process the certificate
                val downloader = CertificateDownloader(context)
                downloader.downloadAndStoreCertificate(url)

                // Notify success on main thread
                withContext(Dispatchers.Main) {
                    showSuccessNotification()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorNotification(e.localizedMessage ?: "Certificate processing failed")
                }
            }
        }
    }

    private fun showSuccessNotification() {
        Toast.makeText(context, "Certificate added successfully", Toast.LENGTH_SHORT).show()
    }

    private fun showErrorNotification(message: String) {
        Toast.makeText(context, "Error: $message", Toast.LENGTH_LONG).show()
    }
}