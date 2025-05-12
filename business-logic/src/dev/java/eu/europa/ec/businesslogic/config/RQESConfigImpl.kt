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

package eu.europa.ec.businesslogic.config

import android.content.Context
import eu.europa.ec.businesslogic.BuildConfig
import eu.europa.ec.eudi.rqes.HashAlgorithmOID
import eu.europa.ec.eudi.rqesui.domain.extension.toUriOrEmpty
import eu.europa.ec.eudi.rqesui.infrastructure.config.DocumentRetrievalConfig
import eu.europa.ec.eudi.rqesui.infrastructure.config.EudiRQESUiConfig
import eu.europa.ec.eudi.rqesui.infrastructure.config.RqesServiceConfig
import eu.europa.ec.eudi.rqesui.infrastructure.config.data.QtspData
import eu.europa.ec.resourceslogic.R
import java.net.URI

class RQESConfigImpl(val context: Context) : EudiRQESUiConfig {

    override val rqesServiceConfig: RqesServiceConfig
        get() = RqesServiceConfig(
            clientId = "wallet-client",
            clientSecret = "somesecret2",
            authFlowRedirectionURI = URI.create(BuildConfig.RQES_DEEPLINK),
            hashAlgorithm = HashAlgorithmOID.SHA_256,
        )

    override val qtsps: List<QtspData>
        get() = listOf(
            QtspData(
                name = "Wallet-Centric",
                endpoint = "https://walletcentric.signer.eudiw.dev/csc/v2".toUriOrEmpty(),
                scaUrl = "https://walletcentric.signer.eudiw.dev",
            )
        )

    override val printLogs: Boolean get() = BuildConfig.DEBUG

    override val documentRetrievalConfig: DocumentRetrievalConfig
        get() = DocumentRetrievalConfig.X509Certificates(
            context,
            listOf(
                R.raw.intermediate_brukersted_ca
            )
        )
}