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

1. Verifier API
2. Issuing API

Via the *WalletCoreConfig* interface inside the business-logic module.

```
interface WalletCoreConfig {
    val config: EudiWalletConfig
}
```

You can configure the *EudiWalletConfig* per flavor. You can find both implementations inside the core-logic module at src/demo/config/WalletCoreConfigImpl and src/dev/config/WalletCoreConfigImpl

```
    private companion object {
        const val VCI_ISSUER_URL = "https://issuer.eudiw.dev/oidc"
        const val VCI_CLIENT_ID = "wallet-demo"
        const val AUTHENTICATION_REQUIRED = false
    }
```

If you plan to use the *ClientIdScheme.Preregistered* for OpenId4VP configuration, please add the following to the configuration files.

```
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

3. Trusted certificates

Via the *WalletCoreConfig* interface.

```
interface WalletCoreConfig {
    val config: EudiWalletConfig
}
```

Same as the Verifier and Issuing APIs you can configure the Trusted certificates for the *EudiWalletConfig* per flavor inside the core-logic module at src/demo/config/WalletCoreConfigImpl and src/dev/config/WalletCoreConfigImpl

```
_config = EudiWalletConfig.Builder(context)
            .trustedReaderCertificates(R.raw.eudi_pid_issuer_ut)
            .build()
```

The application's IACA certificates are located [here](https://github.com/eu-digital-identity-wallet/eudi-app-android-wallet-ui/tree/main/resources-logic/src/main/res/raw):

## DeepLink Schemas configuration

According to the specifications issuance and presentation require deep-linking for the same device flows.

If you want to adjust any schema, you can alter the *AndroidLibraryConventionPlugin* inside the build-logic module.

```
val eudiOpenId4VpScheme = "eudi-openid4vp"
val eudiOpenid4VpHost = "*"

val mdocOpenId4VpScheme = "mdoc-openid4vp"
val mdocOpenid4VpHost = "*"

val openId4VpScheme = "openid4vp"
val openid4VpHost = "*"

val credentialOfferScheme = "openid-credential-offer"
val credentialOfferHost = "*"
```

Let's assume you want to change the credential offer schema to custom-my-offer:// the *AndroidLibraryConventionPlugin* should look like this:

```
val eudiOpenId4VpScheme = "eudi-openid4vp"
val eudiOpenid4VpHost = "*"

val mdocOpenId4VpScheme = "mdoc-openid4vp"
val mdocOpenid4VpHost = "*"

val openId4VpScheme = "openid4vp"
val openid4VpHost = "*"

val credentialOfferScheme = "custom-my-offer"
val credentialOfferHost = "*"
```

In case of an additive change, e.g. adding an extra credential offer schema, you must adjust the following.

AndroidLibraryConventionPlugin:

```
val credentialOfferScheme = "openid-credential-offer"
val credentialOfferHost = "*"

val myOwnCredentialOfferScheme = "custom-my-offer"
val myOwnCredentialOfferHost = "*"
```

```
// Manifest placeholders used for OpenId4VCI
manifestPlaceholders["credentialOfferHost"] = credentialOfferHost
manifestPlaceholders["credentialOfferScheme"] = credentialOfferScheme
manifestPlaceholders["myOwnCredentialOfferHost"] = myOwnCredentialOfferHost
manifestPlaceholders["myOwnCredentialOfferScheme"] = myOwnCredentialOfferScheme
```

```
addConfigField("CREDENTIAL_OFFER_SCHEME", credentialOfferScheme)
addConfigField("MY_OWN_CREDENTIAL_OFFER_SCHEME", myOwnCredentialOfferScheme)
```

Android Manifest (inside assembly-logic module):

```
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

```
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
    EXTERNAL(listOf("external"))
}
```

In the case of an additive change regarding openId4VP, you also need to update the *EudiWalletConfig* for each flavor inside the core-logic module.

```
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

Currently, the application supports specific docTypes for scoped issuance (On the Add Document screen the pre-configured buttons like *National ID*, *Driving License*, and *Age verification*).

The supported list and user interface are not configurable because, with the credential offer, you can issue any format-supported document.

To extend the supported list and display a new button for your document, please follow the instructions below.

In *DocumentIdentifier*, inside the core-logic module, you must add a new data object to the sealed interface with your namespace and doctype.

Example:

```
data object YOUR_OWN : DocumentIdentifier {
    override val nameSpace: String
        get() = "your_own_name_space"
    override val docType: DocType
        get() = "your_own_doc_type"
}
```

After completing the above change, please address all compilation errors within the *DocumentIdentifier*. Ensure that all when statements are exhausted, adhering to the established pattern used in the other documents for consistency.

Example:

```
fun DocumentIdentifier.isSupported(): Boolean {
    return when (this) {
        is DocumentIdentifier.PID, DocumentIdentifier.MDL, DocumentIdentifier.AGE, DocumentIdentifier.YOUR_OWN -> true
        is DocumentIdentifier.SAMPLE, is DocumentIdentifier.OTHER -> false
    }
}
```

In *strings.xml*. inside resources-logic module, add a new string for document title localization

Example:

```
<!-- Document Types -->
<string name="pid">National ID</string>
<string name="mdl">Driving License</string>
<string name="age_verification">Age Verification</string>
<string name="load_sample_data">Load Sample Documents</string>
<string name="your_own_document_title">Your own document title</string>
```

In *DocumentTypeUi*, inside the common-feature module, please adjust the extension function *DocumentIdentifier.toUiName* to point to a string localization for the document Title.

Example:

```
fun DocumentIdentifier.toUiName(resourceProvider: ResourceProvider): String {
    return when (this) {
        is DocumentIdentifier.PID -> resourceProvider.getString(R.string.pid)
        is DocumentIdentifier.MDL -> resourceProvider.getString(R.string.mdl)
        is DocumentIdentifier.AGE -> resourceProvider.getString(R.string.age_verification)
        is DocumentIdentifier.SAMPLE -> resourceProvider.getString(R.string.load_sample_data)
        is DocumentIdentifier.YOUR_OWN -> resourceProvider.getString(R.string.your_own_document_title)
        is DocumentIdentifier.OTHER -> docType
    }
}
```

In *TestsData*, inside common-feature module, please address all compilation errors. Ensure that all when statements are exhausted, adhering to the established pattern used in the other documents for consistency.

In *AddDocumentInteractor*, inside issuance-feature module, please adjust the *getAddDocumentOption* function to add your new document.

Example:

```
val options = mutableListOf(
     DocumentOptionItemUi(
        text = DocumentIdentifier.PID.toUiName(resourceProvider),
        icon = AppIcons.Id,
        type = DocumentIdentifier.PID,
        available = true
    ),
    DocumentOptionItemUi(
        text = DocumentIdentifier.MDL.toUiName(resourceProvider),
        icon = AppIcons.Id,
        type = DocumentIdentifier.MDL,
        available = canCreateExtraDocument(flowType)
    ),
    DocumentOptionItemUi(
        text = DocumentIdentifier.AGE.toUiName(resourceProvider),
        icon = AppIcons.Id,
        type = DocumentIdentifier.AGE,
        available = canCreateExtraDocument(flowType)
    ),
    DocumentOptionItemUi(
        text = DocumentIdentifier.YOUR_OWN.toUiName(resourceProvider),
        icon = AppIcons.Id,
        type = DocumentIdentifier.YOUR_OWN,
        available = canCreateExtraDocument(flowType)
    )
)
```

## How to work with self-signed certificates

This section describes configuring the application to interact with services utilizing self-signed certificates.

1. Open the build.gradle.kts file of the "core-logic" module.
2. In the 'dependencies' block add the following two:
    ```
    implementation(libs.ktor.android)
    implementation(libs.ktor.logging)
    ```
3. Now, you need to navigate to the **ConfigWalletCoreImpl.kt** file, either in:
   *src\dev\java\eu\europa\ec\corelogic\config* or
   *src\demo\java\eu\europa\ec\corelogic\config*
   depending on the flavor of your choice.
4. Here, add these imports:
    ```
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
    ```
5. Add a custom HttpClient that allows self-signed certificates
    ```
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
6. Finally, add this custom HttpClient to the config, by appending it to the EudiWalletConfig.Builder, with the following lines:
    ```
   .ktorHttpClientFactory {
        ProvideKtorHttpClient.client()
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

```
interface StorageConfig {
    val pinStorageProvider: PinStorageProvider
    val biometryStorageProvider: BiometryStorageProvider
}
```

You can provide your storage implementation by implementing the *PinStorageProvider* interface and then setting it as default to the *StorageConfigImpl* pinStorageProvider variable.
The project utilizes Koin for Dependency Injection (DI), thus requiring adjustment of the *LogicAuthenticationModule* graph to provide the configuration.

Implementation Example:
```
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
```
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
```
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

1. Initializing the provider (e.g. Firebase, Appcenter, etc...)
2. Screen logging
3. Event logging

Via the *AnalyticsConfig* inside the analytics-logic module.

```
interface AnalyticsConfig {
    val analyticsProviders: Map<String, AnalyticsProvider>
        get() = emptyMap()
}
```

You can provide your implementation by implementing the *AnalyticsProvider* interface and then adding it to your *AnalyticsConfigImpl* analyticsProviders variable.
You will also need the provider's token/key, thus requiring a Map<String, AnalyticsProvider> configuration.
The project utilizes Koin for Dependency Injection (DI), thus requiring adjustment of the *LogicAnalyticsModule* graph to provide the configuration.

Implementation Example:
```
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
```
class AnalyticsConfigImpl : AnalyticsConfig {
    override val analyticsProviders: Map<String, AnalyticsProvider>
        get() = mapOf("YOUR_OWN_KEY" to AppCenterAnalyticsProvider)
}
```

Config Construction via Koin DI Example:
```
@Single
fun provideAnalyticsConfig(): AnalyticsConfig = AnalyticsConfigImpl()
```
