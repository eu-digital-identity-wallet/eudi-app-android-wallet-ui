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

package eu.europa.ec.corelogic.util

import android.annotation.SuppressLint
import android.util.Log
import java.security.SecureRandom
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Utility for configuring SSL trust settings in development environments.
 * WARNING: Do not use in production as it disables certificate validation.
 */
object SslDevUtility {
    private const val TAG = "SslDevUtility"
    private var isInitialized = false

    /**
     * Configures the JVM to trust all SSL certificates.
     * This should only be used in development/testing environments.
     */
    @SuppressLint("TrustAllX509TrustManager", "CustomX509TrustManager")
    fun trustAllCertificates() {
        if (isInitialized) {
            Log.d(TAG, "SSL trust already initialized")
            return
        }

        try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<java.security.cert.X509Certificate>,
                        authType: String
                    ) {
                        Log.d(TAG, "checkClientTrusted called for: $authType")
                    }

                    override fun checkServerTrusted(
                        chain: Array<java.security.cert.X509Certificate>,
                        authType: String
                    ) {
                        Log.d(TAG, "checkServerTrusted called for: $authType")
                    }

                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                        return arrayOf()
                    }
                }
            )

            // Create and initialize SSL context
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())

            // Set as default SSL socket factory
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)

            // Set default hostname verifier to accept all hostnames
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }

            isInitialized = true
            Log.d(TAG, "SSL trust configuration initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set up SSL trust configuration", e)
        }
    }
}