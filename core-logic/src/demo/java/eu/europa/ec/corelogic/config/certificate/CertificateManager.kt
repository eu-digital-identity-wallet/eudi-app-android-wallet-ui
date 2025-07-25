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
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import android.util.Base64
import java.io.ByteArrayInputStream

class CertificateManager(private val context: Context) {

    val preferenceKey = "certificates"

    fun getStoredCertificates(): List<X509Certificate> {
        val sharedPreferences = context.getSharedPreferences("certificates", Context.MODE_PRIVATE)
        val certificateSet = sharedPreferences.getStringSet(preferenceKey, emptySet()) ?: emptySet()

        return certificateSet.mapNotNull { base64Cert ->
            try {
                base64ToCertificate(base64Cert)
            } catch (e: Exception) {
                // Log error if needed
                null // Skip invalid certificates
            }
        }
    }

    private fun base64ToCertificate(base64Cert: String): X509Certificate {
        val bytes = Base64.decode(base64Cert, Base64.DEFAULT)
        val certFactory = CertificateFactory.getInstance("X.509")

        // Handle both PEM and DER formats
        val certString = String(bytes)
        return if (certString.contains("-----BEGIN CERTIFICATE-----")) {
            // PEM format
            val pemContent = certString
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replace("\n", "")
                .replace("\r", "")

            val decodedBytes = Base64.decode(pemContent, Base64.DEFAULT)
            certFactory.generateCertificate(ByteArrayInputStream(decodedBytes)) as X509Certificate
        } else {
            // DER format
            certFactory.generateCertificate(ByteArrayInputStream(bytes)) as X509Certificate
        }
    }

    companion object {
        // Helper function to quickly get certificates from any context
        fun getCertificates(context: Context): List<X509Certificate> {
            return CertificateManager(context).getStoredCertificates()
        }
    }
}