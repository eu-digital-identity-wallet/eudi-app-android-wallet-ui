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
import eu.europa.ec.corelogic.model.DocumentCategories
import eu.europa.ec.corelogic.model.DocumentCategory
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ClientIdScheme
import eu.europa.ec.eudi.wallet.transfer.openId4vp.EncryptionAlgorithm
import eu.europa.ec.eudi.wallet.transfer.openId4vp.EncryptionMethod
import eu.europa.ec.eudi.wallet.transfer.openId4vp.Format
import eu.europa.ec.resourceslogic.R

internal class WalletCoreConfigImpl(
    private val context: Context
) : WalletCoreConfig {

    private companion object {
        const val VCI_ISSUER_URL = "https://issuer.eudiw.dev"
        const val VCI_CLIENT_ID = "wallet-dev"
        const val AUTHENTICATION_REQUIRED = false
    }

    private var _config: EudiWalletConfig? = null

    override val config: EudiWalletConfig
        get() {
            if (_config == null) {
                _config = EudiWalletConfig {
                    configureDocumentKeyCreation(
                        userAuthenticationRequired = AUTHENTICATION_REQUIRED,
                        userAuthenticationTimeout = 30_000L,
                        useStrongBoxForKeys = true
                    )
                    configureOpenId4Vp {
                        withEncryptionAlgorithms(listOf(EncryptionAlgorithm.ECDH_ES))
                        withEncryptionMethods(
                            listOf(
                                EncryptionMethod.A128CBC_HS256,
                                EncryptionMethod.A256GCM
                            )
                        )

                        withClientIdSchemes(
                            listOf(ClientIdScheme.X509SanDns)
                        )
                        withSchemes(
                            listOf(
                                BuildConfig.OPENID4VP_SCHEME,
                                BuildConfig.EUDI_OPENID4VP_SCHEME,
                                BuildConfig.MDOC_OPENID4VP_SCHEME
                            )
                        )
                        withFormats(
                            Format.MsoMdoc, Format.SdJwtVc.ES256
                        )
                    }

                    configureOpenId4Vci {
                        withIssuerUrl(issuerUrl = VCI_ISSUER_URL)
                        withClientId(clientId = VCI_CLIENT_ID)
                        withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
                        withParUsage(OpenId4VciManager.Config.ParUsage.IF_SUPPORTED)
                        withUseDPoPIfSupported(true)
                    }

                    configureReaderTrustStore(context, R.raw.eudi_pid_issuer_ut)
                }
            }
            return _config!!
        }

    override val documentsCategories: DocumentCategories
        get() = DocumentCategories(
            value = mapOf(
                DocumentCategory.Government to listOf(
                    DocumentIdentifier.MdocPid,
                    DocumentIdentifier.SdJwtPid,
                    DocumentIdentifier.OTHER(
                        formatType = "org.iso.18013.5.1.mDL"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.tax.1"
                    ),
                    DocumentIdentifier.MdocPseudonym,
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.pseudonym.age_over_18.deferred_endpoint"
                    )
                ),
                DocumentCategory.Travel to listOf(
                    DocumentIdentifier.OTHER(
                        formatType = "org.iso.23220.2.photoid.1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "org.iso.18013.5.1.reservation"
                    ),
                ),
                DocumentCategory.Finance to listOf(
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.iban.1"
                    )
                ),
                DocumentCategory.Education to emptyList(),
                DocumentCategory.Health to listOf(
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.hiid.1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.ehic.1"
                    ),
                ),
                DocumentCategory.SocialSecurity to listOf(
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.samplepda1.1"
                    )
                ),
                DocumentCategory.Retail to listOf(
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.loyalty.1"
                    ),
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.msisdn.1"
                    ),
                ),
                DocumentCategory.Other to listOf(
                    DocumentIdentifier.OTHER(
                        formatType = "eu.europa.ec.eudi.por.1"
                    )
                ),
            )
        )
}