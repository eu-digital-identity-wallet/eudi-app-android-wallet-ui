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
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.transfer.openid4vp.ClientIdScheme
import eu.europa.ec.eudi.wallet.transfer.openid4vp.EncryptionAlgorithm
import eu.europa.ec.eudi.wallet.transfer.openid4vp.EncryptionMethod
import eu.europa.ec.eudi.wallet.transfer.openid4vp.PreregisteredVerifier

internal class WalletCoreConfigImpl(
    private val context: Context
) : WalletCoreConfig {

    companion object {
        const val VERIFIER_API_URI = "https://dev.verifier.eudiw.dev"
        const val VCI_ISSUER_URL = "https://preprod.issuer.eudiw.dev/oidc"
        const val VCI_CLIENT_ID = "wallet-dev"
    }

    private var _config: EudiWalletConfig? = null

    override val config: EudiWalletConfig
        get() {
            if (_config == null) {
                _config = EudiWalletConfig.Builder(context)
                    .openId4VpConfig {
                        withEncryptionAlgorithms(listOf(EncryptionAlgorithm.ECDH_ES))
                        withEncryptionMethods(listOf(EncryptionMethod.A128CBC_HS256))

                        withClientIdSchemes(
                            listOf(
                                ClientIdScheme.X509SanDns,
                                ClientIdScheme.Preregistered(
                                    listOf(
                                        PreregisteredVerifier(
                                            "Verifier",
                                            VERIFIER_API_URI
                                        )
                                    )
                                )
                            )
                        )
                    }
                    .openId4VciConfig {
                        withIssuerUrl(issuerUrl = VCI_ISSUER_URL)
                        withClientId(clientId = VCI_CLIENT_ID)
                    }
                    .build()
            }
            return _config!!
        }
}