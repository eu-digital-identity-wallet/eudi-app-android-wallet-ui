# How to configure the application

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
        const val OPENID4VP_VERIFIER_API_URI = "https://verifier.eudiw.dev"
        const val OPENID4VP_VERIFIER_LEGAL_NAME = "EUDI Remote Verifier"
        const val OPENID4VP_VERIFIER_CLIENT_ID = "Verifier"
        const val VCI_ISSUER_URL = "https://issuer.eudiw.dev/oidc"
        const val VCI_CLIENT_ID = "wallet-demo"
        const val AUTHENTICATION_REQUIRED = false
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

The application's certificates are located here:

https://github.com/niscy-eudiw/eudi-app-android-wallet-ui/tree/main/resources-logic/src/main/res/raw

You will also find the IACA certificate here. (trusted iaca root certificates).

## DeepLink Schemas configuration

According to the specifications issuance and presentation require deep-linking for the same device flows.

If you want to adjust any schema you can do it by altering the *AndroidLibraryConventionPlugin* inside the build-logic module.

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

In case of an additive change, e.g. adding an extra credential offer schema, you need to adjust the following.

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
