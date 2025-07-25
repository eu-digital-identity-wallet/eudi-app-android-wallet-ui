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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import android.util.Base64
import androidx.core.content.edit

class CertificateDownloader(private val context: Context) {

    val preferenceKey = "certificates"

    suspend fun downloadAndStoreCertificate(urlString: String) {
        // Download the certificate data
        val pemData = downloadDataFromUrl(urlString)

        // Parse and validate the certificate
        val certificate = parsePemCertificate(pemData)

        // If we got here, validation passed - store Base64 encoded version
        val base64Cert = Base64.encodeToString(pemData, Base64.DEFAULT)
        storeCertificateInPreferences(base64Cert)
    }

    private suspend fun downloadDataFromUrl(urlString: String): ByteArray =
        withContext(Dispatchers.IO) {
            var connection: HttpsURLConnection? = null
            var inputStream: InputStream? = null

            try {
                val url = URL(urlString)
                connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
                    inputStream = connection.inputStream
                    inputStream.readBytes()
                } else {
                    throw Exception("HTTP error code: ${connection.responseCode}")
                }
            } finally {
                inputStream?.close()
                connection?.disconnect()
            }
        }

    private fun parsePemCertificate(pemData: ByteArray): X509Certificate {
        // Remove PEM headers/footers if present
        var certData = String(pemData)
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()

        // Decode Base64 if necessary (some PEM files are already Base64 encoded)
        val decodedBytes = if (certData == String(pemData)) {
            pemData // wasn't PEM formatted, use raw bytes
        } else {
            Base64.decode(certData, Base64.DEFAULT)
        }

        val certFactory = CertificateFactory.getInstance("X.509")
        val cert = certFactory.generateCertificate(ByteArrayInputStream(decodedBytes)) as X509Certificate

        // Perform validation
        cert.checkValidity() // throws exception if not valid

        return cert
    }

    private fun storeCertificateInPreferences(base64Cert: String) {
        val sharedPreferences = context.getSharedPreferences("certificates", Context.MODE_PRIVATE)
        val currentSet = sharedPreferences.getStringSet(preferenceKey, mutableSetOf()) ?: mutableSetOf()

        // Create a new set to avoid SharedPreferences modification issues
        val newSet = HashSet(currentSet)
        newSet.add(base64Cert)

        sharedPreferences.edit() {
            putStringSet(preferenceKey, newSet)
        }
    }
}