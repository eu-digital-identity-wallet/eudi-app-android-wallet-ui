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

package eu.europa.ec.corelogic.config

import android.content.Context
import eu.europa.ec.corelogic.BuildConfig
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ClientIdScheme
import eu.europa.ec.eudi.wallet.transfer.openId4vp.Format
import eu.europa.ec.resourceslogic.R
import kotlin.time.Duration.Companion.seconds

internal class WalletCoreConfigImpl(
    private val context: Context
) : WalletCoreConfig {

    private var _config: EudiWalletConfig? = null

    override val config: EudiWalletConfig
        get() {
            if (_config == null) {
                _config = EudiWalletConfig {
                    configureDocumentKeyCreation(
                        userAuthenticationRequired = false,
                        userAuthenticationTimeout = 30.seconds,
                        useStrongBoxForKeys = true
                    )
                    configureOpenId4Vp {
                        withClientIdSchemes(
                            listOf(
                                ClientIdScheme.X509SanDns,
                                ClientIdScheme.X509Hash
                            )
                        )
                        withSchemes(
                            listOf(
                                BuildConfig.OPENID4VP_SCHEME,
                                BuildConfig.EUDI_OPENID4VP_SCHEME,
                                BuildConfig.MDOC_OPENID4VP_SCHEME
                            )
                        )
                        withFormats(
                            Format.MsoMdoc.ES256, Format.SdJwtVc.ES256
                        )
                    }

                    configureReaderTrustStore(
                        context,
                        R.raw.pidissuerca02_cz,
                        R.raw.pidissuerca02_ee,
                        R.raw.pidissuerca02_eu,
                        R.raw.pidissuerca02_lu,
                        R.raw.pidissuerca02_nl,
                        R.raw.pidissuerca02_pt,
                        R.raw.pidissuerca02_ut,
                        R.raw.dc4eu
                    )
                }
            }
            return _config!!
        }

    override val vciConfig: List<OpenId4VciManager.Config>
        get() = listOf(
            OpenId4VciManager.Config.Builder()
                .withIssuerUrl(issuerUrl = "https://ec.dev.issuer.eudiw.dev")
                .withClientId(clientId = "wallet-dev")
                .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
                .withParUsage(OpenId4VciManager.Config.ParUsage.IF_SUPPORTED)
                .withDPoPUsage(OpenId4VciManager.Config.DPoPUsage.IfSupported())
                .build(),
            OpenId4VciManager.Config.Builder()
                .withIssuerUrl(issuerUrl = "https://dev.issuer-backend.eudiw.dev")
                .withClientId(clientId = "wallet-dev")
                .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
                .withParUsage(OpenId4VciManager.Config.ParUsage.IF_SUPPORTED)
                .withDPoPUsage(OpenId4VciManager.Config.DPoPUsage.IfSupported())
                .build()
        )
}