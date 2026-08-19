/*
 * Copyright (c) 2026 European Commission
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

import eu.europa.ec.corelogic.BuildConfig
import eu.europa.ec.corelogic.model.DocumentIdentifier
import eu.europa.ec.corelogic.provider.RegistrationCheckProvider
import eu.europa.ec.eudi.etsi119602.datamodel.Uri
import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationClassifications
import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifier
import eu.europa.ec.eudi.etsi1196x2.consultation.AttestationIdentifierPredicate
import eu.europa.ec.eudi.etsi1196x2.consultation.SupportedLists
import eu.europa.ec.eudi.iso18013.transfer.response.ReaderAuthPolicy
import eu.europa.ec.eudi.openid4vci.CredentialReusePolicies
import eu.europa.ec.eudi.openid4vci.EudiReusePolicyType
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.dcapi.DCAPIProtocol
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig
import eu.europa.ec.eudi.wallet.registration.issuer.IssuerRegistrationPolicy
import eu.europa.ec.eudi.wallet.registration.relyingparty.WrpRegistrationPolicy
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ClientIdScheme
import eu.europa.ec.eudi.wallet.transfer.openId4vp.Format
import eu.europa.ec.eudi.wallet.trust.TrustPolicy
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class WalletCoreConfigImpl(
    private val registrationCheckProvider: RegistrationCheckProvider,
) : WalletCoreConfig {

    private var _config: EudiWalletConfig? = null

    override val isRegistrationCheckEnabled: Boolean by lazy {
        runBlocking { registrationCheckProvider.isEnabled() }
    }

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
                                BuildConfig.MDOC_OPENID4VP_SCHEME,
                                BuildConfig.HAIP_OPENID4VP_SCHEME
                            )
                        )
                        withFormats(
                            Format.MsoMdoc.ES256, Format.SdJwtVc.ES256
                        )
                    }

                    configureDCAPI {
                        withEnabled(true)
                        withSupportedProtocols(
                            DCAPIProtocol.ISO_MDOC,
                            DCAPIProtocol.OPENID4VP_V1_SIGNED,
                        )
                    }

                    configureEtsiTrust {
                        loteLocations(
                            SupportedLists(
                                pidProviders = Uri("https://trustedlist.serviceproviders.eudiw.dev/LOTE/json/PIDProviders.jwt"),
                                wrpacProviders = Uri("https://trustedlist.serviceproviders.eudiw.dev/LOTE/json/WRPACProviders.jwt"),
                                wrprcProviders = Uri("https://trustedlist.serviceproviders.eudiw.dev/LOTE/json/WRPRCProviders.jwt"),
                                pubEaaProviders = Uri("https://trustedlist.serviceproviders.eudiw.dev/LOTE/json/PubEAAProviders.jwt"),
                            )
                        )

                        classifications(
                            AttestationClassifications(
                                pids = AttestationIdentifierPredicate.any(
                                    identifiers = setOf(
                                        AttestationIdentifier.MDoc(
                                            docType = DocumentIdentifier.MdocPid.formatType
                                        ),
                                        AttestationIdentifier.SDJwtVc(
                                            vct = DocumentIdentifier.SdJwtPid.formatType
                                        ),
                                    )
                                )
                            )
                        )

                        relaxCertificateProfiles()
                        relaxPkixRevocation()
                    }

                    configureIssuerTrust {
                        policy { default(TrustPolicy.Action.ENFORCE) }
                        requireSignedMetadata()
                        configureIssuerRegistrationPolicy(
                            if (isRegistrationCheckEnabled) {
                                IssuerRegistrationPolicy.Enabled
                            } else {
                                IssuerRegistrationPolicy.Disabled
                            }
                        )
                    }

                    configureDocumentStatusResolver {
                        configureTrust {
                            policy {
                                default(TrustPolicy.Action.INFORM)
                            }
                        }
                    }

                    configureReaderTrustStore {
                        readerAuthPolicy(ReaderAuthPolicy.EnforceIfPresent)
                    }

                    configureWrpRegistrationPolicy(
                        if (isRegistrationCheckEnabled) {
                            WrpRegistrationPolicy.Enabled
                        } else {
                            WrpRegistrationPolicy.Disabled
                        }
                    )
                }
            }
            return _config!!
        }

    override val issuersConfig: List<VciConfig>
        get() = listOf(
            VciConfig(
                issuerUrl = "https://issuer.eudiw.dev",
                config = OpenId4VciManager.Config.Builder()
                    .withClientAuthenticationType(
                        OpenId4VciManager.ClientAuthenticationType.AttestationBased(
                            clientId = "eudiw-abca"
                        )
                    )
                    .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
                    .withParUsage(OpenId4VciManager.Config.ParUsage.IF_SUPPORTED)
                    .withDPopConfig(DPopConfig.Default)
                    .withSupportedCredentialReusePolicies(
                        CredentialReusePolicies.Supported(
                            policyTypes = setOf(
                                EudiReusePolicyType.RotatingBatch,
                                EudiReusePolicyType.OnceOnly,
                                EudiReusePolicyType.LimitedTime,
                            )
                        )
                    )
                    .build(),
                order = 0
            ),
            VciConfig(
                issuerUrl = "https://issuer-backend.eudiw.dev",
                config = OpenId4VciManager.Config.Builder()
                    .withClientAuthenticationType(
                        OpenId4VciManager.ClientAuthenticationType.AttestationBased(
                            clientId = "eudiw-abca"
                        )
                    )
                    .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
                    .withParUsage(OpenId4VciManager.Config.ParUsage.IF_SUPPORTED)
                    .withDPopConfig(DPopConfig.Default)
                    .withSupportedCredentialReusePolicies(
                        CredentialReusePolicies.Supported(
                            policyTypes = setOf(
                                EudiReusePolicyType.RotatingBatch,
                                EudiReusePolicyType.OnceOnly,
                                EudiReusePolicyType.LimitedTime,
                            )
                        )
                    )
                    .build(),
                order = 1
            )
        )

    override val documentIssuanceConfig: DocumentIssuanceConfig
        get() = DocumentIssuanceConfig(
            defaultPolicy = CredentialPolicy.RotatingBatch(
                numberOfCredentials = 1,
                reissueTriggerLifetimeLeft = 24.hours
            ),
            documentSpecificPolicies = mapOf(
                DocumentIdentifier.MdocPid to CredentialPolicy.OnceOnly(
                    numberOfCredentials = 10,
                    reissueTriggerUnused = 2
                ),
                DocumentIdentifier.SdJwtPid to CredentialPolicy.OnceOnly(
                    numberOfCredentials = 10,
                    reissueTriggerUnused = 2
                ),
            ),
            reissuanceRule = ReIssuanceRule(
                backgroundInterval = 15.minutes
            )
        )

    override val walletProviderHost: String
        get() = "https://wallet-provider.eudiw.dev"
}