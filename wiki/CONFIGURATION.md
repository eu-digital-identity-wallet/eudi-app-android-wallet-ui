# How to configure the application

## Table of contents

* [General configuration](#general-configuration)
* [Dependency versions](#dependency-versions)
* [Production configuration reference](#production-configuration-reference)
* [Deep link scheme configuration](#deep-link-scheme-configuration)
* [Scoped Issuance Document Configuration](#scoped-issuance-document-configuration)
* [How to work with self-signed certificates](#how-to-work-with-self-signed-certificates)
* [Theme configuration](#theme-configuration)
* [Pin Storage configuration](#pin-storage-configuration)
* [Analytics configuration](#analytics-configuration)

## General configuration

All core network and trust settings are centralized in the `WalletCoreConfig` interface inside the
**core-logic** module:

```kotlin
interface WalletCoreConfig {
    // 1. Wallet Core SDK configuration, including trust, OpenID4VP, DCAPI, and key settings.
    val config: EudiWalletConfig

    // 2. Issuing APIs.
    val issuersConfig: List<VciConfig>

    // 3. Document category grouping used by the UI.
    val documentCategories: DocumentCategories

    // 4. Revocation/status check interval.
    val revocationInterval: Duration

    // 5. Credential issuance and re-issuance policy.
    val documentIssuanceConfig: DocumentIssuanceConfig

    // 6. Wallet Provider Host.
    val walletProviderHost: String
}
```

You configure these properties **per flavor** by providing a `WalletCoreConfigImpl` for each build
variant:

* `core-logic/src/demo/java/eu/europa/ec/corelogic/config/WalletCoreConfigImpl.kt`
* `core-logic/src/dev/java/eu/europa/ec/corelogic/config/WalletCoreConfigImpl.kt`

Each flavor can use different issuer configs, wallet provider hosts, and trust stores.

1. Issuing API

   The Issuing API is configured via the `issuersConfig` property:

    ```kotlin
    override val issuersConfig: List<VciConfig>
        get() = listOf(
            VciConfig(
                config = OpenId4VciManager.Config.Builder()
                    .withIssuerUrl(issuerUrl = "https://ec.dev.issuer.eudiw.dev")
                    .withClientAuthenticationType(OpenId4VciManager.ClientAuthenticationType.AttestationBased)
                    .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
                    .withParUsage(OpenId4VciManager.Config.ParUsage.IF_SUPPORTED)
                    .withDPopConfig(DPopConfig.Default)
                    .build(),
                order = 0
            ),
            VciConfig(
                config = OpenId4VciManager.Config.Builder()
                    .withIssuerUrl(issuerUrl = "https://dev.issuer-backend.eudiw.dev")
                    .withClientAuthenticationType(OpenId4VciManager.ClientAuthenticationType.AttestationBased)
                    .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
                    .withParUsage(OpenId4VciManager.Config.ParUsage.IF_SUPPORTED)
                    .withDPopConfig(DPopConfig.Default)
                    .build(),
                order = 1
            )
        )
    ```

   Adjust the configuration per flavor in the corresponding `WalletCoreConfigImpl`. The `order`
   property controls how issuers are displayed in the add-document flow.

2. Wallet Provider Host

   The Wallet Provider Host is configured via the `walletProviderHost` property:

    ```kotlin
    override val walletProviderHost: String
        get() = "https://dev.wallet-provider.eudiw.dev"
    ```

   Again, set a different value per flavor in the corresponding `WalletCoreConfigImpl`. This host is
   used by wallet attestation flows and must point to the wallet provider service for the selected
   environment.

3. Trusted certificates

   Trusted certificates are configured via the `config` property:

    ```kotlin
    _config = EudiWalletConfig {
       configureReaderTrustStore(context, R.raw.pidissuerca02_ut)
    }
    ```

   The application's IACA certificates are
   located [here](https://github.com/eu-digital-identity-wallet/eudi-app-android-wallet-ui/tree/main/resources-logic/src/main/res/raw)

   Configure `EudiWalletConfig` per flavor inside the appropriate `WalletCoreConfigImpl`. Demo and
   development trust anchors must be replaced before production.

4. Preregistered Client Scheme

   If you plan to use the *ClientIdScheme.Preregistered* for OpenId4VP configuration, please add the
   following to the configuration files.

    ```kotlin
    const val OPENID4VP_VERIFIER_API_URI = "your_verifier_url"
    const val OPENID4VP_VERIFIER_LEGAL_NAME = "your_verifier_legal_name"
    const val OPENID4VP_VERIFIER_CLIENT_ID = "your_verifier_client_id"
    
    configureOpenId4Vp {
        withClientIdSchemes(
            listOf(
                ClientIdScheme.Preregistered(
                    listOf(
                        PreregisteredVerifier(
                            clientId = OPENID4VP_VERIFIER_CLIENT_ID,
                            verifierApi = OPENID4VP_VERIFIER_API_URI,
                            legalName = OPENID4VP_VERIFIER_LEGAL_NAME
                        )
                    )
                )
            )
        )
    }
    ```

5. RQES

   Via the *ConfigLogic* interface inside the business-logic module.

    ```kotlin
    interface ConfigLogic {
        /**
         * RQES Config.
         */
        val rqesConfig: EudiRQESUiConfig
    }
    ```

   You can configure the *RQESConfig*, which implements the EudiRQESUiConfig interface from the
   RQESUi SDK, per flavor. Both implementations are inside the business-logic module at
   `business-logic/src/demo/java/eu/europa/ec/businesslogic/config/RQESConfigImpl.kt` and
   `business-logic/src/dev/java/eu/europa/ec/businesslogic/config/RQESConfigImpl.kt`.

    ```kotlin
    class RQESConfigImpl : EudiRQESUiConfig {
    
        // Optional. Default English translations will be used if not set.
        override val translations: Map<String, Map<LocalizableKey, String>> get()
    
        // Optional. Default theme will be used if not set.
        override val themeManager: ThemeManager get()
    
        override val qtsps: List<QtspData> get()
    
        // Optional. Default is false.
        override val printLogs: Boolean get()
    
        override val documentRetrievalConfig: DocumentRetrievalConfig get()
    }
    ```

   Example:

    ```kotlin
    class RQESConfigImpl : EudiRQESUiConfig {
    
        override val qtsps: List<QtspData>
            get() = listOf(
                QtspData(
                    name = "your_name",
                    endpoint = "your_endpoint".toUri(),
                    tsaUrl = "your_tsaUrl",
                    clientId = "your_clientid",
                    clientSecret = "your_secret_or_non_confidential_demo_value",
                    authFlowRedirectionURI = URI.create("your_uri"),
                    hashAlgorithm = HashAlgorithmOID.SHA_256,
                )
            )
    
        override val printLogs: Boolean get() = BuildConfig.DEBUG
    
        override val documentRetrievalConfig: DocumentRetrievalConfig
            get() = DocumentRetrievalConfig.X509Certificates(
                context = context,
                certificates = listOf(R.raw.my_certificate),
                shouldLog = should_log_option
            )
    }
    ```

   Do not hardcode real production OAuth client secrets in the mobile app. Prefer a public-client
   profile, backend mediation, or another pattern approved by the QTSP and security team.

6. Wallet Activation

   You can enable or disable the PID Wallet Activation flow. If you choose to enable this feature, the Wallet will not be operational unless a PID is issued first.
   With this feature disabled, there are no such limitations, and the Wallet can operate without a PID being issued beforehand.

   Via the *ConfigLogic* interface inside the business-logic module.

   ```kotlin
   interface ConfigLogic {
   
         /**
         * Set if the wallet requires PID Activation.
         */
        val forcePidActivation: Boolean get() = false
   }
    ```

## Dependency versions

The central version catalog is:

`gradle/libs.versions.toml`

At the time this guide was written, key versions included:

| Dependency | Current repo version |
| --- | --- |
| Android Gradle Plugin | `9.2.1` |
| Kotlin | `2.3.21` |
| EUDI Wallet Core | `0.28.0` |
| EUDI RQES UI SDK | `0.3.8` |
| Ktor | `3.4.3` |
| SQLCipher Android | `4.16.0` |
| OWASP Dependency Check | `12.2.2` |

Review these versions whenever you change configuration that depends on SDK behavior, such as
Wallet Core issuance/presentation, RQES, Ktor networking, SQLCipher storage, or dependency-check
policy. For production release rules, see [GO_LIVE.md](GO_LIVE.md#dependency-versions).

## Production configuration reference

The following table summarizes the main values an implementer must review before release. For a
complete production process, see [GO_LIVE.md](GO_LIVE.md).

| Configuration | Where it is defined | What to put in production |
| --- | --- | --- |
| App ID | `app/build.gradle.kts` | A reverse-DNS package name owned by the implementer, for example `eu.example.wallet`. Do not change after public release unless publishing a separate app. |
| App name suffix | `build-logic/convention/src/main/kotlin/project/convention/logic/AppFlavor.kt` | Empty for production. Keep suffixes only for dev/test builds. |
| Build flavor | `AppFlavor.kt` and matching source sets | Add a dedicated production flavor, for example `prod`, instead of reusing `dev` or `demo`. |
| Issuer URLs | `core-logic/src/<flavor>/java/.../WalletCoreConfigImpl.kt` | HTTPS URLs for approved production OpenID4VCI issuers. Include scheme and port if non-default. |
| Issuer order | `VciConfig(order = ...)` | Integer display order in the add-document flow. Use stable ordering for user support and screenshots. |
| Wallet Provider host | `walletProviderHost` in `WalletCoreConfigImpl.kt` | HTTPS base URL for the production wallet provider/attestation service. |
| OpenID4VCI redirect URI | `BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK` from build-logic placeholders | A registered URI accepted by the issuer and handled only by the wallet app. |
| OpenID4VP schemes | `AndroidLibraryConventionPlugin.kt` and `EudiWalletConfig.configureOpenId4Vp` | Schemes and client ID schemes approved for the ecosystem. Keep the manifest, `BuildConfig`, and Wallet Core config aligned. |
| Reader/verifier trust anchors | `configureReaderTrustStore(...)` raw resources | Production IACA/reader/verifier trust anchors from an approved trust list or governance process. |
| Document key settings | `configureDocumentKeyCreation(...)` | For LoA High PID and other high-assurance EAA/QEAA credentials, require strong user authentication and hardware-backed key protection unless an approved remote high-assurance key protection design replaces local key use. Use `0.seconds` only when one prompt per key use is acceptable; for batch issuing, a short approved window such as `10.seconds` may be needed. |
| Document key storage | `EudiWallet.Builder` in `core-logic/src/main/java/.../LogicCoreModule.kt` | Default Wallet Core behavior uses Android Keystore secure areas. Use `withSecureAreas(...)`, `withStorage(...)`, or `withDocumentManager(...)` if production requires an alternative secure area, remote-backed key service, or custom document manager. |
| Wallet attestation key storage | `EudiWallet.Builder.withWalletKeyManager(...)` and Wallet Provider policy | Use if wallet attestation/client-attestation keys must be generated, stored, attested, or unlocked by a custom secure area or remote high-assurance key service. |
| DPoP key storage | `DPopConfig.Default` or `DPopConfig.Custom(...)` in each issuer config | Prefer DPoP where supported. Use `DPopConfig.Custom(...)` when issuance proof-of-possession keys require custom secure area, StrongBox, user authentication, or issuer-specific key policy. |
| Remote presentation ephemeral key handling | OpenID4VP/presentation-manager integration | Ephemeral protocol key material must be generated per transaction, not reused across verifiers, and not persisted beyond the protocol flow. If the Wallet Core version exposes a dedicated ephemeral key-storage option, configure it in the production `EudiWallet` or presentation-manager integration and document the exact SDK API. |
| DCAPI | `configureDCAPI { withEnabled(...) }` | Enable only if the production wallet supports Digital Credential API flows and has tested them. |
| Document issuance rules | `documentIssuanceConfig` | Credential rotation, one-time-use, quantity, and re-issuance intervals agreed with issuer capacity and privacy policy. |
| Revocation interval | `revocationInterval` | A value that balances user experience, battery/network use, and relying-party risk. |
| RQES QTSP endpoint | `business-logic/src/<flavor>/java/.../RQESConfigImpl.kt` | Production CSC/QTSP endpoint provided by the selected qualified trust service provider. |
| RQES TSA URL | `RQESConfigImpl.kt` | Production timestamp authority URL required by the QTSP/signature profile. |
| RQES client ID/secret | `RQESConfigImpl.kt` | Do not hardcode confidential secrets in the app. Use an approved public-client or backend-mediated design. |
| RQES redirect URI | `BuildConfig.RQES_DEEPLINK` | Redirect URI registered with the QTSP and declared in the Android manifest. |
| RQES document retrieval trust | `DocumentRetrievalConfig` | Production certificates or trust material required for retrieving signing documents. |
| PIN storage | `authentication-logic` and `StorageConfig` | Confirm PBKDF2 parameters, lockout policy, encrypted preferences, and migration behavior meet policy. |
| Database key/storage | `storage-logic` and encrypted `PrefsController` | Ensure database keys are generated securely, encrypted at rest, excluded from backup, and migrated safely. |
| Network logging | `network-logic/.../NetworkModule.kt` | `LogLevel.NONE` for release. Never log tokens, credentials, signatures, or document data. |
| Network security config | `network-logic/src/main/res/xml/network_security_config.xml` | Cleartext disabled. No debug CA or trust-all logic in release. |
| Analytics providers | `analytics-logic` | Only approved providers, with data minimization, consent, retention, and no credential contents. |
| Release signing | `app/build.gradle.kts` and CI secrets | Keystore and passwords controlled through CI secret storage, HSM/KMS, or an equivalent process. |

## Deep link scheme configuration

According to the specifications, issuance, presentation, and RQES require deep-linking for the same device flows.

If you want to adjust any scheme, you can alter the *AndroidLibraryConventionPlugin* inside the build-logic module.

```kotlin
val eudiOpenId4VpScheme = "eudi-openid4vp"
val eudiOpenid4VpHost = "*"

val mdocOpenId4VpScheme = "mdoc-openid4vp"
val mdocOpenid4VpHost = "*"

val openId4VpScheme = "openid4vp"
val openid4VpHost = "*"

val haipOpenId4VpScheme = "haip-vp"
val haipOpenid4VpHost = "*"

val credentialOfferScheme = "openid-credential-offer"
val credentialOfferHost = "*"

val credentialOfferHaipScheme = "haip-vci"
val credentialOfferHaipHost = "*"

val rqesScheme = "rqes"
val rqesHost = "oauth"
val rqesPath = "/callback"

val rqesDocRetrievalScheme = "eudi-rqes"
val rqesDocRetrievalHost = "*"
```

The reference configuration uses broad wildcard hosts for several custom-scheme links. For
production, restrict hosts and paths where the protocol allows it, register redirect URIs with the
issuer/verifier/QTSP, and validate every inbound URI before acting on it.

Let's assume you want to change the credential offer scheme to custom-my-offer:// the *AndroidLibraryConventionPlugin* should look like this:

```kotlin
val eudiOpenId4VpScheme = "eudi-openid4vp"
val eudiOpenid4VpHost = "*"

val mdocOpenId4VpScheme = "mdoc-openid4vp"
val mdocOpenid4VpHost = "*"

val openId4VpScheme = "openid4vp"
val openid4VpHost = "*"

val haipOpenId4VpScheme = "haip-vp"
val haipOpenid4VpHost = "*"

val credentialOfferScheme = "custom-my-offer"
val credentialOfferHost = "*"

val credentialOfferHaipScheme = "haip-vci"
val credentialOfferHaipHost = "*"
```

In case of an additive change, e.g., adding an extra credential offer scheme, you must adjust the following.

AndroidLibraryConventionPlugin:

```kotlin
val credentialOfferScheme = "openid-credential-offer"
val credentialOfferHost = "*"

val credentialOfferHaipScheme = "haip-vci"
val credentialOfferHaipHost = "*"

val myOwnCredentialOfferScheme = "custom-my-offer"
val myOwnCredentialOfferHost = "*"
```

```kotlin
// Manifest placeholders used for OpenId4VCI
manifestPlaceholders["credentialOfferHost"] = credentialOfferHost
manifestPlaceholders["credentialOfferScheme"] = credentialOfferScheme
manifestPlaceholders["credentialOfferHaipHost"] = credentialOfferHaipHost
manifestPlaceholders["credentialOfferHaipScheme"] = credentialOfferHaipScheme
manifestPlaceholders["myOwnCredentialOfferHost"] = myOwnCredentialOfferHost
manifestPlaceholders["myOwnCredentialOfferScheme"] = myOwnCredentialOfferScheme
```

```kotlin
addConfigField("CREDENTIAL_OFFER_SCHEME", credentialOfferScheme)
addConfigField("CREDENTIAL_OFFER_HAIP_SCHEME", credentialOfferHaipScheme)
addConfigField("MY_OWN_CREDENTIAL_OFFER_SCHEME", myOwnCredentialOfferScheme)
```

Android Manifest (inside assembly-logic module):

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />

    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <data
        android:host="${credentialOfferHost}"
        android:scheme="${credentialOfferScheme}" />

</intent-filter>

<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    
    <data
        android:host="${credentialOfferHaipHost}"
        android:scheme="${credentialOfferHaipScheme}" />

</intent-filter>

<intent-filter>
    <action android:name="android.intent.action.VIEW" />

    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <data
        android:host="${myOwnCredentialOfferHost}"
        android:scheme="${myOwnCredentialOfferScheme}" />

</intent-filter>
```

DeepLinkType (DeepLinkHelper Object inside ui-logic module):

```kotlin
enum class DeepLinkType(val schemas: List<String>, val host: String? = null) {

    OPENID4VP(
        schemas = listOf(
            BuildConfig.OPENID4VP_SCHEME,
            BuildConfig.EUDI_OPENID4VP_SCHEME,
            BuildConfig.MDOC_OPENID4VP_SCHEME,
            BuildConfig.HAIP_OPENID4VP_SCHEME
        )
    ),
    CREDENTIAL_OFFER(
        schemas = listOf(
            BuildConfig.CREDENTIAL_OFFER_SCHEME,
            BuildConfig.CREDENTIAL_OFFER_HAIP_SCHEME,
            BuildConfig.MY_OWN_CREDENTIAL_OFFER_SCHEME
        )
    ),
    ISSUANCE(
        schemas = listOf(BuildConfig.ISSUE_AUTHORIZATION_SCHEME),
        host = BuildConfig.ISSUE_AUTHORIZATION_HOST
    ),
    DYNAMIC_PRESENTATION(
        emptyList()
    ),
    RQES(
        schemas = listOf(BuildConfig.RQES_SCHEME),
        host = BuildConfig.RQES_HOST
    ),
    RQES_DOC_RETRIEVAL(
        schemas = listOf(BuildConfig.RQES_DOC_RETRIEVAL_SCHEME)
    ),
    EXTERNAL(emptyList())
}
```

In the case of an additive change regarding OpenID4VP, you also need to update the *EudiWalletConfig* for each flavor inside the core-logic module.

```kotlin
configureOpenId4Vp {
   withSchemes(
      listOf(
         BuildConfig.OPENID4VP_SCHEME,
         BuildConfig.EUDI_OPENID4VP_SCHEME,
         BuildConfig.MDOC_OPENID4VP_SCHEME, 
         BuildConfig.HAIP_OPENID4VP_SCHEME,
         BuildConfig.YOUR_OWN_OPENID4VP_SCHEME
      )
   )
}
```

## Scoped Issuance Document Configuration

The credential configuration is derived directly from the issuer's metadata.
The issuer URL is configured per flavor via the *issuersConfig* property inside the core-logic
module at `core-logic/src/demo/java/eu/europa/ec/corelogic/config/WalletCoreConfigImpl.kt` and
`core-logic/src/dev/java/eu/europa/ec/corelogic/config/WalletCoreConfigImpl.kt`.
The *order* property determines the order the issuers appear into, in the *AddDocumentScreen*, if
more than one exists.
If you want to add or adjust the displayed scoped documents, you must modify the issuer's metadata, and the wallet will automatically resolve your changes.

## How to work with self-signed certificates

This section describes configuring the application to interact with local services that use
self-signed or private-CA certificates.

Do not disable TLS validation. Do not add a trust-all `X509TrustManager`. Do not set
`HostnameVerifier { _, _ -> true }`. Those patterns make the app vulnerable to trivial
man-in-the-middle attacks and must not be present in release builds.

For local development, use a debug-only trust anchor:

1. Create a local development CA.
2. Use that CA to sign the TLS certificates for your local issuer, verifier, and wallet-provider
   services.
3. Add the CA certificate to a debug-only resource such as
   `network-logic/src/debug/res/raw/local_dev_ca.cer`.
4. Add a debug-only `network_security_config.xml` under
   `network-logic/src/debug/res/xml/network_security_config.xml`.
5. Configure only the local development host or domain to trust that CA:

   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <network-security-config>
       <domain-config cleartextTrafficPermitted="false">
           <domain includeSubdomains="true">10.0.2.2</domain>
           <trust-anchors>
               <certificates src="@raw/local_dev_ca" />
           </trust-anchors>
       </domain-config>
       <base-config cleartextTrafficPermitted="false">
           <trust-anchors>
               <certificates src="system" />
           </trust-anchors>
       </base-config>
   </network-security-config>
   ```

6. Keep `network-logic/src/main/res/xml/network_security_config.xml` strict for release builds:

   ```xml
   <network-security-config>
       <base-config cleartextTrafficPermitted="false" />
   </network-security-config>
   ```

If a temporary trust-all client is needed for a one-off experiment, keep it out of committed source
or isolate it in a debug-only source set that cannot be compiled into release. Add CI checks that
search for `TrustAll`, `X509TrustManager`, `HostnameVerifier { _, _ -> true }`, and
`cleartextTrafficPermitted="true"` before release.

## Theme configuration

The application allows the configuration of:

1. Colors
2. Images
3. Shape
4. Fonts
5. Dimension

Via *ThemeManager.Builder()*.

## Pin Storage configuration

The application allows the configuration of the PIN storage. You can configure the following:

1. Where the pin will be stored
2. From where the pin will be retrieved
3. Pin matching and validity

Via the *StorageConfig* inside the authentication-logic module.

The reference implementation uses `PrefsPinStorageProvider`, which stores a PBKDF2-HMAC-SHA256
hash with a random salt and an iteration count, backed by the encrypted `PrefsController`. The
encrypted preferences use Jetpack DataStore with Google Tink AEAD and an Android Keystore master
key. For production, confirm the iteration count, lockout behavior, device binding, migration,
backup exclusion, and recovery policy against your security requirements.

```kotlin
interface StorageConfig {
    val pinStorageProvider: PinStorageProvider
    val biometryStorageProvider: BiometryStorageProvider
}
```

You can provide your storage implementation by implementing the *PinStorageProvider* interface and then setting it as the default to the *StorageConfigImpl* pinStorageProvider variable.
The project utilizes Koin for Dependency Injection (DI), thus requiring adjustment of the *LogicAuthenticationModule* graph to provide the configuration.

Implementation Example:

```kotlin
class PrefsPinStorageProvider(
    private val prefsController: PrefsController
) : PinStorageProvider {

    override suspend fun hasPin(): Boolean {
       // Implementation
    }

    override suspend fun setPin(pin: SecurePin) {
       // Implementation
    }

    override suspend fun isPinValid(pin: SecurePin): Boolean {
       // Implementation
    }
}
```

Config Example:

```kotlin
class StorageConfigImpl(
    private val pinImpl: PinStorageProvider,
    private val biometryImpl: BiometryStorageProvider
) : StorageConfig {
    override val pinStorageProvider: PinStorageProvider
        get() = pinImpl
    override val biometryStorageProvider: BiometryStorageProvider
        get() = biometryImpl
}
```

Config Construction via Koin DI Example:

```kotlin
@Single
fun provideStorageConfig(
    prefsController: PrefsController
): StorageConfig = StorageConfigImpl(
    pinImpl = PrefsPinStorageProvider(prefsController),
    biometryImpl = PrefsBiometryStorageProvider(prefsController)
)
```

## Analytics configuration

The application allows the configuration of multiple analytics providers. You can configure the following:

1. Initializing the provider (e.g., Firebase, Appcenter, etc)
2. Screen logging
3. Event logging

Via the *AnalyticsConfig* inside the analytics-logic module.

No analytics provider is enabled by default. If you add one, also add the required SDK dependency,
document the provider's data processing role, avoid logging credential contents or verifier request
payloads, and align telemetry with consent, retention, and privacy requirements.

```kotlin
interface AnalyticsConfig {
    val analyticsProviders: Map<String, AnalyticsProvider>
        get() = emptyMap()
}
```

You can provide your implementation by implementing the *AnalyticsProvider* interface and then adding it to your *AnalyticsConfigImpl* analyticsProviders variable.
You will also need the provider's token/key, thus requiring a Map<String, AnalyticsProvider> configuration.
The project utilizes Koin for Dependency Injection (DI), thus requiring adjustment of the *LogicAnalyticsModule* graph to provide the configuration.

Implementation Example:

```kotlin
object AppCenterAnalyticsProvider : AnalyticsProvider {
    override fun initialize(context: Application, key: String) {
        AppCenter.start(
            context,
            key,
            Analytics::class.java
        )
    }

    override fun logScreen(name: String, arguments: Map<String, String>) {
        logEvent(name, arguments)
    }

    override fun logEvent(event: String, arguments: Map<String, String>) {
        if (Analytics.isEnabled().get()) {
            Analytics.trackEvent(event, arguments)
        }
    }
}
```

Config Example:

```kotlin
class AnalyticsConfigImpl : AnalyticsConfig {
    override val analyticsProviders: Map<String, AnalyticsProvider>
        get() = mapOf("YOUR_OWN_KEY" to AppCenterAnalyticsProvider)
}
```

Config Construction via Koin DI Example:

```kotlin
@Single
fun provideAnalyticsConfig(): AnalyticsConfig = AnalyticsConfigImpl()
```
