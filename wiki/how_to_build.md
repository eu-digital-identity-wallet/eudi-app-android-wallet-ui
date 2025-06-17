# Building the Reference apps to interact with issuing and verifying services.
## Table of contents
* [Overview](#overview)
* [Setup Apps](#setup-apps)
* [How to work with self signed certificates](#how-to-work-with-self-signed-certificates)
## Overview
This guide aims to assist developers build the Android application.

## Setup Apps
### EUDI Android Wallet reference application
You need [Android Studio](https://developer.android.com/studio) and its associated tools installed on your machine. We recommend the latest stable version.
Clone the [Android repository](https://github.com/eu-digital-identity-wallet/eudi-app-android-wallet-ui)
Open the project in Android Studio.

The application has two product flavors:
- "Dev", which communicates with the services deployed in an environment based on the latest main branch.
- "Demo", which communicates with the services deployed in an environment based on the latest main branch.

and two Build Types:
- "Debug", which has full logging enabled.
- "Release", which has no logging enabled.

which, ultimately, result in the following Build Variants:

- "devDebug", "devRelease", "demoDebug", "demoRelease" .

To change the Build Variant, go to Build -> Select Build Variant and from the tool window you can click on the "Active Build Variant" of the module ":app" and select the one you prefer.
It will automatically apply it for the other modules as well.

To run the App on a device, firstly you must connect your device with the Android Studio, and then go to Run -> Run 'app'.
To run the App on an emulator, simply go to Run -> Run 'app'.

### Running with remote services
The app is configured to use some configuration in the two ***ConfigWalletCoreImpl.kt*** files (located in the "**core-logic**" module, in either
*src\dev\java\eu\europa\ec\corelogic\config* or
*src\demo\java\eu\europa\ec\corelogic\config*,
depending on the flavor of your choice).

These are the contents of the ConfigWalletCoreImpl file (dev flavor) and you don't need to change anything:
```Kotlin
private companion object {
        const val VCI_ISSUER_URL = "https://dev.issuer.eudiw.dev"
        const val VCI_CLIENT_ID = "wallet-dev"
        const val AUTHENTICATION_REQUIRED = false
}
```

### Running with local services
The first step here is to have all three services running locally on your machine,
you can follow these Repositories for further instructions:
* [Issuer](https://github.com/eu-digital-identity-wallet/eudi-srv-web-issuing-eudiw-py)
* [Web Verifier UI](https://github.com/eu-digital-identity-wallet/eudi-web-verifier)
* [Web Verifier Endpoint](https://github.com/eu-digital-identity-wallet/eudi-srv-web-verifier-endpoint-23220-4-kt)


After this, and assuming you are now running everything locally,
you need to change the contents of the ConfigWalletCoreImpl file, from:
```Kotlin
private companion object {
        const val VCI_ISSUER_URL = "https://dev.issuer.eudiw.dev"
        const val VCI_CLIENT_ID = "wallet-dev"
        const val AUTHENTICATION_REQUIRED = false
}
```
into something like this:
```Kotlin
private companion object {
        const val VCI_ISSUER_URL = "local_IP_address_of_issuer"
        const val VCI_CLIENT_ID = "wallet-dev"
        const val AUTHENTICATION_REQUIRED = false
}
```

for example:
```Kotlin
private companion object {
        const val VCI_ISSUER_URL = "https://192.168.1.1:5000"
        const val VCI_CLIENT_ID = "wallet-dev"
        const val AUTHENTICATION_REQUIRED = false
}
```

Finally, you have to also change the content of ***network_security_config.xml*** file and allow HTTP traffic, to this:
```Xml
<network-security-config>
    <base-config cleartextTrafficPermitted="true" />
</network-security-config>
```

## How to work with self-signed certificates

This section describes configuring the application to interact with services utilizing self-signed certificates.

1. Open the build.gradle.kts file of the "core-logic" module.
2. In the 'dependencies' block add the following two:
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

For all configuration options please refer to [this document](configuration.md)
