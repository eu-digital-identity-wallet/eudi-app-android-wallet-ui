# Building The Reference Apps To Interact With Issuing And Verifying Services

## Table of contents

* [Overview](#overview)
* [Prerequisites](#prerequisites)
* [Build variants](#build-variants)
* [Build commands](#build-commands)
* [Running with remote services](#running-with-remote-services)
* [Running with local services](#running-with-local-services)
* [Why 10.0.2.2?](#why-10022)
* [How to work with self-signed certificates](#how-to-work-with-self-signed-certificates)
* [Production note](#production-note)

## Overview

This guide helps developers build the Android Wallet application and connect it to either the
hosted reference services or locally running issuer/verifier services.

For production deployment, use this file only as a build reference. Production teams must also
follow [the go-live guide](go_live.md).

## Prerequisites

Install or prepare:

* Android Studio, preferably the current stable version.
* Android SDK Platform 36, because the project currently compiles with `compileSdk = 36`.
* Android SDK Build Tools compatible with Android Gradle Plugin used in `gradle/libs.versions.toml`.
* JDK 17. The build logic configures Java/Kotlin target 17.
* Git.
* A physical Android device or emulator running Android 10/API 29 or higher.

You do not need to install Gradle separately. Use the checked-in Gradle wrapper:

```powershell
.\gradlew.bat --version
```

On macOS/Linux:

```bash
./gradlew --version
```

Clone and open the project:

```bash
git clone https://github.com/eu-digital-identity-wallet/eudi-app-android-wallet-ui.git
cd eudi-app-android-wallet-ui
```

Then open the folder in Android Studio.

## Build variants

The application currently has two product flavors:

* `dev`: development/reference service configuration. The application ID has the `.dev` suffix.
* `demo`: demo/reference service configuration.

The application has two build types:

* `debug`: debuggable, no minification, verbose network logging.
* `release`: not debuggable, minified, network logging disabled, signed with the configured release
  signing config.

The resulting app build variants are:

* `devDebug`
* `devRelease`
* `demoDebug`
* `demoRelease`

To select a variant in Android Studio, open **Build > Select Build Variant** and choose the active
variant for the `:app` module. Android Studio will apply the matching variants to the dependent
modules.

## Build commands

From the project root on Windows:

```powershell
.\gradlew.bat :app:assembleDevDebug
.\gradlew.bat :app:assembleDemoDebug
```

Release builds:

```powershell
.\gradlew.bat :app:assembleDevRelease
.\gradlew.bat :app:assembleDemoRelease
```

On macOS/Linux, replace `.\gradlew.bat` with `./gradlew`.

Run tests and checks:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat dependencyCheckAnalyze
```

APK outputs are created under:

```text
app/build/outputs/apk/<flavor>/<build-type>/
```

Examples:

```text
app/build/outputs/apk/dev/debug/app-dev-debug.apk
app/build/outputs/apk/demo/release/app-demo-release.apk
```

Release signing is configured in `app/build.gradle.kts`. The current reference setup expects a
keystore file at the project root named `sign` and reads signing values from `local.properties` or
environment variables. For production signing, see [go_live.md](go_live.md#release-signing).

## Running with remote services

The app is configured through `WalletCoreConfigImpl.kt` files in the `core-logic` module:

* `core-logic/src/dev/java/eu/europa/ec/corelogic/config/WalletCoreConfigImpl.kt`
* `core-logic/src/demo/java/eu/europa/ec/corelogic/config/WalletCoreConfigImpl.kt`

The current `dev` flavor uses reference development services similar to:

```kotlin
.withIssuerUrl(issuerUrl = "https://ec.dev.issuer.eudiw.dev")
.withIssuerUrl(issuerUrl = "https://dev.issuer-backend.eudiw.dev")

override val walletProviderHost: String
    get() = "https://dev.wallet-provider.eudiw.dev"
```

The current `demo` flavor uses reference demo services similar to:

```kotlin
.withIssuerUrl(issuerUrl = "https://issuer.eudiw.dev")
.withIssuerUrl(issuerUrl = "https://issuer-backend.eudiw.dev")

override val walletProviderHost: String
    get() = "https://wallet-provider.eudiw.dev"
```

These values are suitable for reference/demo testing only. They are not production values.

To run the app against the hosted services:

1. Select `devDebug` or `demoDebug`.
2. Connect a device or start an emulator.
3. Run the `:app` configuration from Android Studio, or install the APK with `adb install`.
4. Follow the user flows in the root `README.md`.

## Running with local services

To test against services running on your own workstation, start the required services first:

* [Issuer](https://github.com/eu-digital-identity-wallet/eudi-srv-web-issuing-eudiw-py)
* [Web Verifier UI](https://github.com/eu-digital-identity-wallet/eudi-web-verifier)
* [Web Verifier Endpoint](https://github.com/eu-digital-identity-wallet/eudi-srv-web-verifier-endpoint-23220-4-kt)

Then update the selected flavor's `WalletCoreConfigImpl.kt`.

For an emulator, use `10.0.2.2` to reach services running on the host machine. Include the scheme
and port used by the local service:

```kotlin
override val issuersConfig: List<VciConfig>
    get() = listOf(
        VciConfig(
            config = OpenId4VciManager.Config.Builder()
                .withIssuerUrl(issuerUrl = "https://10.0.2.2:8443")
                .withClientAuthenticationType(OpenId4VciManager.ClientAuthenticationType.AttestationBased)
                .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
                .withParUsage(OpenId4VciManager.Config.ParUsage.IF_SUPPORTED)
                .withDPopConfig(DPopConfig.Default)
                .build(),
            order = 0
        )
    )

override val walletProviderHost: String
    get() = "https://10.0.2.2:8444"
```

For a physical device, use the host computer's LAN IP address instead:

```kotlin
.withIssuerUrl(issuerUrl = "https://192.168.1.50:8443")
```

The issuer/verifier metadata, redirect URIs, and wallet deep links must match the app's configured
schemes and hosts. See [configuration.md](configuration.md) for those values.

## Why 10.0.2.2?

When using the Android emulator, `10.0.2.2` is a special alias that routes to `localhost` on your
development machine. If your issuer is running locally on your host, the emulator can access it via
`https://10.0.2.2:<port>`.

Physical devices do not understand `10.0.2.2`. Use your workstation's reachable LAN IP address or a
controlled development DNS name.

## How to work with self-signed certificates

For local development, prefer trusting a local development CA over disabling TLS validation.

Do not add a trust-all `X509TrustManager`, do not set
`HostnameVerifier { _, _ -> true }`, and do not permit cleartext traffic in production or in code
that can be accidentally built into a release artifact.

Recommended local-development approach:

1. Create or obtain a local development CA certificate.
2. Sign the local issuer/verifier/wallet-provider TLS certificates with that CA.
3. Add the CA certificate to a debug-only resource, for example:

   ```text
   network-logic/src/debug/res/raw/local_dev_ca.cer
   ```

4. Add a debug-only network security config, for example:

   ```text
   network-logic/src/debug/res/xml/network_security_config.xml
   ```

5. Configure that debug-only file to trust the local CA:

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

6. Keep the release network security config strict. The existing production-facing base config should
   keep `cleartextTrafficPermitted="false"` and should not trust debug-only anchors.

If you need an emergency trust-all client for a short local experiment, keep it outside committed
source or behind an explicit debug-only source set that cannot be compiled into release. Remove it
before opening a pull request.

## Production note

The hosted reference values, local IP addresses, self-signed certificates, debug CAs, and demo trust
anchors described here are not production configuration.

Before launching a real wallet, create a dedicated production flavor, replace all endpoints and
trust anchors, verify signing and release checks, and follow [go_live.md](go_live.md).
