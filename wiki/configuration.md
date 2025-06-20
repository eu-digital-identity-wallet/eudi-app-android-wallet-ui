# How to configure the application

## Table of contents

* [General configuration](#general-configuration)
* [DeepLink Schemas configuration](#deeplink-schemas-configuration)
* [Scoped Issuance Document Configuration](#scoped-issuance-document-configuration)
* [How to work with self-signed certificates](#how-to-work-with-self-signed-certificates)
* [Theme configuration](#theme-configuration)
* [Pin Storage configuration](#pin-storage-configuration)
* [Analytics configuration](#analytics-configuration)

## General configuration

The application allows the configuration of:

1. Issuing API

Via the *WalletCoreConfig* interface inside the business-logic module.

```Kotlin
interface WalletCoreConfig {
    val config: EudiWalletConfig
}
```

You can configure the *EudiWalletConfig* per flavor. You can find both implementations inside the core-logic module at src/demo/config/WalletCoreConfigImpl and src/dev/config/WalletCoreConfigImpl

```Kotlin
    private companion object {
        const val VCI_ISSUER_URL = "https://issuer.eudiw.dev"
        const val VCI_CLIENT_ID = "wallet-demo"
        const val AUTHENTICATION_REQUIRED = false
    }
```

2. Trusted certificates

Via the *WalletCoreConfig* interface.

```Kotlin
interface WalletCoreConfig {
    val config: EudiWalletConfig
}
```

Same as the Verifier and Issuing APIs, you can configure the Trusted certificates for the *EudiWalletConfig* per flavor inside the core-logic module at src/demo/config/WalletCoreConfigImpl and src/dev/config/WalletCoreConfigImpl

```Kotlin
_config = EudiWalletConfig {
   configureReaderTrustStore(context, R.raw.eudi_pid_issuer_ut)
}
```

The application's IACA certificates are located [here](https://github.com/eu-digital-identity-wallet/eudi-app-android-wallet-ui/tree/main/resources-logic/src/main/res/raw)

3. Preregistered Client Scheme

If you plan to use the *ClientIdScheme.Preregistered* for OpenId4VP configuration, please add the following to the configuration files.

```Kotlin
const val OPENID4VP_VERIFIER_API_URI = "your_verifier_url"
const val OPENID4VP_VERIFIER_LEGAL_NAME = "your_verifier_legal_name"
const val OPENID4VP_VERIFIER_CLIENT_ID = "your_verifier_client_id"

.openId4VpConfig {
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

4. RQES

Via the *ConfigLogic* interface inside the business-logic module.

```Kotlin
interface ConfigLogic {
    /**
     * RQES Config.
     */
    val rqesConfig: EudiRQESUiConfig
}
```

You can configure the *RQESConfig*, which implements the EudiRQESUiConfig interface from the RQESUi SDK, per flavor. Both implementations are inside the business-logic module at src/demo/config/RQESConfigImpl and src/dev/config/RQESConfigImpl.

```Kotlin
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

```Kotlin
class RQESConfigImpl : EudiRQESUiConfig {

    override val qtsps: List<QtspData>
        get() = listOf(
            QtspData(
                name = "your_name",
                endpoint = "your_endpoint".toUri(),
                scaUrl = "your_sca",
                clientId = "your_clientid",
                clientSecret = "your_secret",
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

## DeepLink Schemas configuration

According to the specifications, issuance, presentation, and RQES require deep-linking for the same device flows.

If you want to adjust any schema, you can alter the *AndroidLibraryConventionPlugin* inside the build-logic module.

```Kotlin
val eudiOpenId4VpScheme = "eudi-openid4vp"
val eudiOpenid4VpHost = "*"

val mdocOpenId4VpScheme = "mdoc-openid4vp"
val mdocOpenid4VpHost = "*"

val openId4VpScheme = "openid4vp"
val openid4VpHost = "*"

val credentialOfferScheme = "openid-credential-offer"
val credentialOfferHost = "*"

val rqesScheme = "rqes"
val rqesHost = "oauth"
val rqesPath = "/callback"

val rqesDocRetrievalScheme = "eudi-rqes"
val rqesDocRetrievalHost = "*"
```

Let's assume you want to change the credential offer schema to custom-my-offer:// the *AndroidLibraryConventionPlugin* should look like this:

```Kotlin
val eudiOpenId4VpScheme = "eudi-openid4vp"
val eudiOpenid4VpHost = "*"

val mdocOpenId4VpScheme = "mdoc-openid4vp"
val mdocOpenid4VpHost = "*"

val openId4VpScheme = "openid4vp"
val openid4VpHost = "*"

val credentialOfferScheme = "custom-my-offer"
val credentialOfferHost = "*"
```

In case of an additive change, e.g., adding an extra credential offer schema, you must adjust the following.

AndroidLibraryConventionPlugin:

```Kotlin
val credentialOfferScheme = "openid-credential-offer"
val credentialOfferHost = "*"

val myOwnCredentialOfferScheme = "custom-my-offer"
val myOwnCredentialOfferHost = "*"
```

```Kotlin
// Manifest placeholders used for OpenId4VCI
manifestPlaceholders["credentialOfferHost"] = credentialOfferHost
manifestPlaceholders["credentialOfferScheme"] = credentialOfferScheme
manifestPlaceholders["myOwnCredentialOfferHost"] = myOwnCredentialOfferHost
manifestPlaceholders["myOwnCredentialOfferScheme"] = myOwnCredentialOfferScheme
```

```Kotlin
addConfigField("CREDENTIAL_OFFER_SCHEME", credentialOfferScheme)
addConfigField("MY_OWN_CREDENTIAL_OFFER_SCHEME", myOwnCredentialOfferScheme)
```

Android Manifest (inside assembly-logic module):

```Xml
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
            android:host="${myOwnCredentialOfferHost}"
            android:scheme="${myOwnCredentialOfferScheme}" />

</intent-filter>
```

DeepLinkType (DeepLinkHelper Object inside ui-logic module):

```Kotlin
enum class DeepLinkType(val schemas: List<String>, val host: String? = null) {

    OPENID4VP(
        schemas = listOf(
            BuildConfig.OPENID4VP_SCHEME,
            BuildConfig.EUDI_OPENID4VP_SCHEME,
            BuildConfig.MDOC_OPENID4VP_SCHEME
        )
    ),
    CREDENTIAL_OFFER(
        schemas = listOf(
            BuildConfig.CREDENTIAL_OFFER_SCHEME,
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

```Kotlin
.openId4VpConfig {
    withScheme(
        listOf(
                BuildConfig.OPENID4VP_SCHEME,
                BuildConfig.EUDI_OPENID4VP_SCHEME,
                BuildConfig.MDOC_OPENID4VP_SCHEME,
                BuildConfig.YOUR_OWN_OPENID4VP_SCHEME
            )
    )
}
```

## Scoped Issuance Document Configuration

The credential configuration is derived directly from the issuer's metadata. The issuer URL is configured per flavor via the *configureOpenId4Vci* method inside the core-logic module at src/demo/config/WalletCoreConfigImpl and src/dev/config/WalletCoreConfigImpl.
If you want to add or adjust the displayed scoped documents, you must modify the issuer's metadata, and the wallet will automatically resolve your changes.

## How to work with self-signed certificates

This section describes configuring the application to interact with services utilizing self-signed certificates.

1. Open the build.gradle.kts file of the "core-logic" module.
2. In the 'dependencies' block, add the following two:
    ```Gradle
    implementation(libs.ktor.android)
    implementation(libs.ktor.logging)
    ```
3. Now, you need to create a new kotlin file *ProvideKtorHttpClient* and place it into the *src\main\java\eu\europa\ec\corelogic\config* package.
4. Copy and paste the following into your newly created *ProvideKtorHttpClient* kotlin file.
    ```Kotlin
    import android.annotation.SuppressLint
    import io.ktor.client.HttpClient
    import io.ktor.client.engine.android.Android
    import io.ktor.client.plugins.logging.Logging
    import java.security.SecureRandom
    import javax.net.ssl.HostnameVerifier
    import javax.net.ssl.SSLContext
    import javax.net.ssl.TrustManager
    import javax.net.ssl.X509TrustManager
    import javax.security.cert.CertificateException
    
    object ProvideKtorHttpClient {

        @SuppressLint("TrustAllX509TrustManager", "CustomX509TrustManager")
        fun client(): HttpClient {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(
                        chain: Array<java.security.cert.X509Certificate>,
                        authType: String
                    ) {
                    }

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(
                        chain: Array<java.security.cert.X509Certificate>,
                        authType: String
                    ) {
                    }

                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                        return arrayOf()
                    }
                }
            )

            return HttpClient(Android) {
                install(Logging)
                engine {
                    requestConfig
                    sslManager = { httpsURLConnection ->
                        httpsURLConnection.sslSocketFactory = SSLContext.getInstance("TLS").apply {
                            init(null, trustAllCerts, SecureRandom())
                        }.socketFactory
                        httpsURLConnection.hostnameVerifier = HostnameVerifier { _, _ -> true }
                    }
                }
            }
        }

    }
    ```
5. Finally, add this custom HttpClient to the EudiWallet provider function *provideEudiWallet* located in *LogicCoreModule.kt*
    ```Kotlin
    @Single
    fun provideEudiWallet(
    context: Context,
    walletCoreConfig: WalletCoreConfig,
    walletCoreLogController: WalletCoreLogController
    ): EudiWallet = EudiWallet(context, walletCoreConfig.config) {
        withLogger(walletCoreLogController)
        // Custom HttpClient
        withKtorHttpClientFactory {
            ProvideKtorHttpClient.client()
        }
    }
    ```

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

```Kotlin
interface StorageConfig {
    val pinStorageProvider: PinStorageProvider
    val biometryStorageProvider: BiometryStorageProvider
}
```

You can provide your storage implementation by implementing the *PinStorageProvider* interface and then setting it as the default to the *StorageConfigImpl* pinStorageProvider variable.
The project utilizes Koin for Dependency Injection (DI), thus requiring adjustment of the *LogicAuthenticationModule* graph to provide the configuration.

Implementation Example:
```Kotlin
class PrefsPinStorageProvider(
    private val prefsController: PrefsController
) : PinStorageProvider {

    override fun retrievePin(): String {
        return prefsController.getString("DevicePin", "")
    }

    override fun setPin(pin: String) {
        prefsController.setString("DevicePin", pin)
    }

    override fun isPinValid(pin: String): Boolean = retrievePin() == pin
}
```

Config Example:
```Kotlin
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
```Kotlin
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

```Kotlin
interface AnalyticsConfig {
    val analyticsProviders: Map<String, AnalyticsProvider>
        get() = emptyMap()
}
```

You can provide your implementation by implementing the *AnalyticsProvider* interface and then adding it to your *AnalyticsConfigImpl* analyticsProviders variable.
You will also need the provider's token/key, thus requiring a Map<String, AnalyticsProvider> configuration.
The project utilizes Koin for Dependency Injection (DI), thus requiring adjustment of the *LogicAnalyticsModule* graph to provide the configuration.

Implementation Example:
```Kotlin
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
```Kotlin
class AnalyticsConfigImpl : AnalyticsConfig {
    override val analyticsProviders: Map<String, AnalyticsProvider>
        get() = mapOf("YOUR_OWN_KEY" to AppCenterAnalyticsProvider)
}
```

Config Construction via Koin DI Example:
```Kotlin
@Single
fun provideAnalyticsConfig(): AnalyticsConfig = AnalyticsConfigImpl()
```
