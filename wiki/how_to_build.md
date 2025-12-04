# Building the Reference apps to interact with issuing and verifying services.
## Table of contents
* [Overview](#overview)
* [Setup Apps](#setup-apps)
* [How to work with self signed certificates](#how-to-work-with-self-signed-certificates)
## Overview
This guide aims to assist developers in building the Android Wallet application.

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

- "devDebug", "devRelease", "demoDebug", "demoRelease".

To change the Build Variant, go to Build -> Select Build Variant and from the tool window you can click on the "Active Build Variant" of the module ":app" and select the one you prefer.
It will automatically apply it to the other modules as well.

To run the App on a device, firstly you must connect your device with the Android Studio, and then go to Run -> Run 'app'.
To run the App on an emulator, simply go to Run -> Run 'app'.

### Running with remote services
The app is configured to use some configuration in the two ***ConfigWalletCoreImpl.kt*** files (located in the "**core-logic**" module, in either
*src\dev\java\eu\europa\ec\corelogic\config* or
*src\demo\java\eu\europa\ec\corelogic\config*,
depending on the flavor of your choice).

These are the contents of the ConfigWalletCoreImpl file (dev flavor), and you don't need to change anything:

```kotlin
override val vciConfig: List<OpenId4VciManager.Config>
    get() = listOf(
       OpenId4VciManager.Config.Builder()
      .withIssuerUrl(issuerUrl = "https://ec.dev.issuer.eudiw.dev")
      .withClientAuthenticationType(OpenId4VciManager.ClientAuthenticationType.AttestationBased)
      .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
      .withParUsage(OpenId4VciManager.Config.ParUsage.IF_SUPPORTED)
      .withDPoPUsage(OpenId4VciManager.Config.DPoPUsage.IfSupported())
      .build()
)
```

### Running with local services
The first step here is to have all three services running locally on your machine,
you can follow these Repositories for further instructions:
* [Issuer](https://github.com/eu-digital-identity-wallet/eudi-srv-web-issuing-eudiw-py)
* [Web Verifier UI](https://github.com/eu-digital-identity-wallet/eudi-web-verifier)
* [Web Verifier Endpoint](https://github.com/eu-digital-identity-wallet/eudi-srv-web-verifier-endpoint-23220-4-kt)


After this, and assuming you are now running everything locally,
you need to change the contents of the ConfigWalletCoreImpl file, from:

```kotlin
override val vciConfig: List<OpenId4VciManager.Config>
    get() = listOf(
       OpenId4VciManager.Config.Builder()
      .withIssuerUrl(issuerUrl = "https://ec.dev.issuer.eudiw.dev")
      .withClientAuthenticationType(OpenId4VciManager.ClientAuthenticationType.AttestationBased)
      .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
      .withParUsage(OpenId4VciManager.Config.ParUsage.IF_SUPPORTED)
      .withDPoPUsage(OpenId4VciManager.Config.DPoPUsage.IfSupported())
      .build()
)
```

with this:

```kotlin
override val vciConfig: List<OpenId4VciManager.Config>
    get() = listOf(
       OpenId4VciManager.Config.Builder()
      .withIssuerUrl(issuerUrl = "local_IP_address_of_issuer")
      .withClientAuthenticationType(OpenId4VciManager.ClientAuthenticationType.AttestationBased)
      .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
      .withParUsage(OpenId4VciManager.Config.ParUsage.IF_SUPPORTED)
      .withDPoPUsage(OpenId4VciManager.Config.DPoPUsage.IfSupported())
      .build()
)
```

for example:

```kotlin
override val vciConfig: List<OpenId4VciManager.Config>
    get() = listOf(
       OpenId4VciManager.Config.Builder()
      .withIssuerUrl(issuerUrl = "https://10.0.2.2")
      .withClientAuthenticationType(OpenId4VciManager.ClientAuthenticationType.AttestationBased)
      .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
      .withParUsage(OpenId4VciManager.Config.ParUsage.IF_SUPPORTED)
      .withDPoPUsage(OpenId4VciManager.Config.DPoPUsage.IfSupported())
      .build()
)
```

## Why 10.0.2.2?

When using the Android emulator, 10.0.2.2 is a special alias that routes to localhost on your development machine.
So if you’re running the issuer locally on your host, the emulator can access it via https://10.0.2.2.

## How to work with self-signed certificates

This section describes configuring the application to interact with services utilizing self-signed certificates.

*To enable support for self-signed certificates, you must customize the existing Ktor `HttpClient`
used by the application.*

1. Open the `NetworkModule.kt` file of the `network-logic` module.
2. Add the following imports:

    ```kotlin
    import android.annotation.SuppressLint
    import java.security.SecureRandom
    import javax.net.ssl.HostnameVerifier
    import javax.net.ssl.SSLContext
    import javax.net.ssl.TrustManager
    import javax.net.ssl.X509TrustManager
    import javax.security.cert.CertificateException
    ```

3. Replace the `provideHttpClient` function with the following:

    ```kotlin
    @SuppressLint("TrustAllX509TrustManager", "CustomX509TrustManager")
    @Single
    fun provideHttpClient(json: Json): HttpClient {
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
            install(ContentNegotiation) {
                json(
                    json = json,
                    contentType = ContentType.Application.Json
                )
            }
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
    ```

4. Finally, you need to use the preregistered clientId scheme instead of X509.
   
   Change this:

   ```kotlin
   withClientIdSchemes(
    listOf(ClientIdScheme.X509SanDns)
   )
    ```
   
   into something like this:

   ```kotlin
   withClientIdSchemes(
    listOf(
        ClientIdScheme.Preregistered(
            preregisteredVerifiers =
                listOf(
                    PreregisteredVerifier(
                        clientId = "Verifier",
                        legalName = "Verifier",
                        verifierApi = "https://10.0.2.2"
                    )
                )
            )
        )
   )
   ```

   For all configuration options, please refer to [this document](configuration.md)
