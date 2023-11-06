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

package eu.europa.ec.eudi.web.lightIssuing

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import java.io.StringWriter
import java.net.URLEncoder
import java.security.cert.X509Certificate
import java.util.Base64
import kotlin.properties.Delegates

/**
 * Eudi PID issuer singleton object
 */
object EudiPidIssuer {

    /**
     * Country
     *
     * @property code
     * @constructor Create empty Country
     */
    enum class Country(val code: String) {
        /**
         * Country code for Foreign Country (Utoptia)
         *
         * @constructor Create empty Fc
         */
        FC("FC"),

        /**
         * Country code for Portugal
         *
         * @constructor Create empty Pt
         */
        PT("PT"),

        /**
         * Country code for eIDAS
         *
         * @constructor Create empty Cw
         */
        CW("CW")
    }

    private const val BASE_URL = "https://preprod.issuer.eudiw.dev/pid/getpid"
    private const val VERSION = "0.1"
    private const val SCHEME = "eudiw"

    private lateinit var onResultListener: (Result) -> Unit

    @JvmStatic
    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var bytes: ByteArray? by Delegates.observable(byteArrayOf()) { _, _, newValue ->
        onResultListener(newValue?.let { Result.Success(it) }
            ?: Result.Failure(Exception("No document received")))

    }

    /**
     * Issues the document. Issues the EU PID document for the given country and certificates.
     *
     * In certificates use the certificates from the wallet for the document to be issued. These
     * certificates contain the device publicKeys that will be used during signing of the document
     * by the issuer.
     *
     * Example:
     * ```
     * package com.example.myapp
     *
     * import androidx.appcompat.app.AppCompatActivity
     * import java.security.cert.X509Certificate
     *
     * class MainActivity : AppCompatActivity() {
     *
     *   // rest of activity code omitted for brevity
     *
     *   fun issueDocument() {
     *     val country = EudiPidIssuer.Country.FC
     *     val certificates = listOf<X509Certificate>(
     *       // list of wallet certificates
     *     )
     *     EudiPidIssuer.issueDocument(this, country, certificates) { result ->
     *       when (result) {
     *         is EudiPidIssuer.Result.Success -> {
     *           val documentBytes = result.documentBytes
     *           // add document to wallet
     *         }
     *
     *         is EudiPidIssuer.Result.Failure -> {
     *           val error = result.throwable
     *           // handle error
     *         }
     *       }
     *     }
     *   }
     * }
     * ```
     *
     * @param activity the context activity. Is needed to launch the Browser
     * @param country the country to issue the document for
     * @param certificates the certificates to use for the document
     * @param onResult the callback to be called when the document is issued
     */
    @JvmStatic
    fun issueDocument(
        activity: ComponentActivity,
        country: Country = Country.FC,
        certificates: Collection<X509Certificate>,
        onResult: (Result) -> Unit,
    ) {
        onResultListener = onResult
        val (certificate, publicKey) = certificates.first()
            .let {
                val certPemObject = PemObject("CERTIFICATE", it.encoded)
                val stringWriter1 = StringWriter()
                PemWriter(stringWriter1).use { pemWriter ->
                    pemWriter.writeObject(certPemObject)
                }

                val pubKeyPemObject = PemObject("PUBLIC KEY", it.publicKey.encoded)
                val stringWriter = StringWriter()
                PemWriter(stringWriter).use { pemWriter ->
                    pemWriter.writeObject(pubKeyPemObject)
                }
                stringWriter1.toString() to stringWriter.toString()
            }.let {
                Base64.getUrlEncoder()
                    .encodeToString(it.first.toByteArray()) to Base64.getUrlEncoder()
                    .encodeToString(it.second.toByteArray())
            }.let {
                URLEncoder.encode(it.first) to URLEncoder.encode(it.second)
            }
        val returnUrl = URLEncoder.encode("$SCHEME://", "UTF-8")

        val issuingURI =
            Uri.parse("$BASE_URL?version=$VERSION&country=${country.code}&certificate=$certificate&device_publickey=$publicKey&returnURL=$returnUrl")

        activity.startActivity(Intent(Intent.ACTION_VIEW, issuingURI).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    /**
     * Result
     *
     * @constructor Create empty Result
     */
    sealed interface Result {
        /**
         * Success issuance result. Contains the document bytes.
         *
         * @property documentBytes
         * @constructor Create empty Success
         */
        class Success(val documentBytes: ByteArray) : Result

        /**
         * Failure issuance result. Contains the throwable.
         *
         * @property throwable
         * @constructor Create empty Failure
         */
        data class Failure(val throwable: Throwable) : Result
    }
}