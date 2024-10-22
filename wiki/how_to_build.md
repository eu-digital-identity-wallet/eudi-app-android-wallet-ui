# Building the Reference apps to interact with issuing and verifying services.
## Table of contents
* [Overview](#overview)
* [Setup Apps](#setup-apps)
* [How to work with self signed certificates](#how-to-work-with-self-signed-certificates)
* [Configuring your android emulator to work with local issuer/verifier](how_to_debug_android.md)
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
```
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
```
private companion object {
        const val VCI_ISSUER_URL = "https://dev.issuer.eudiw.dev"
        const val VCI_CLIENT_ID = "wallet-dev"
        const val AUTHENTICATION_REQUIRED = false
}
```
into something like this:
```
private companion object {
        const val VCI_ISSUER_URL = "local_IP_address_of_issuer"
        const val VCI_CLIENT_ID = "wallet-dev"
        const val AUTHENTICATION_REQUIRED = false
}
```

for example:
```
private companion object {
        const val VCI_ISSUER_URL = "https://192.168.1.1:5000"
        const val VCI_CLIENT_ID = "wallet-dev"
        const val AUTHENTICATION_REQUIRED = false
}
```

Finally, you have to also change the content of ***network_security_config.xml*** file and allow HTTP traffic, to this:
```
<network-security-config>
    <base-config cleartextTrafficPermitted="true" />
</network-security-config>
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

For all configuration options please refer to [this document](configuration.md)
