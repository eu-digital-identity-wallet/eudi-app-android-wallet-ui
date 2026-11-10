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
follow [the go-live guide](GO_LIVE.md).

## Prerequisites

Install or prepare:

* Android Studio, preferably the current stable version.
* Android SDK Platform 37, because the project currently compiles with `compileSdk = 37`.
* Android SDK Build Tools compatible with Android Gradle Plugin used in `gradle/libs.versions.toml`.
* JDK 17. The build logic configures Java/Kotlin target 17. Unit tests run on JDK 21, which Gradle
  downloads through the toolchain resolver if it is missing.
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

The baseline-profile plugin also adds `*BenchmarkRelease` and `*NonMinifiedRelease` variants, used
only for profiling.

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

`dependencyCheckAnalyze` reads `NVD_API_KEY` from `local.properties` or the environment.

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
environment variables. In the current Gradle file, `storePassword` is read from the same
`androidKeyPassword` / `ANDROID_KEY_PASSWORD` value used for `keyPassword`; if your keystore uses a
separate store password, update the Gradle signing config before building release artifacts. For
production signing, see [GO_LIVE.md](GO_LIVE.md#release-signing).

## Running with remote services

The app is configured through `WalletCoreConfigImpl.kt` files in the `core-logic` module:

* `core-logic/src/dev/java/eu/europa/ec/corelogic/config/WalletCoreConfigImpl.kt`
* `core-logic/src/demo/java/eu/europa/ec/corelogic/config/WalletCoreConfigImpl.kt`

The current `dev` flavor uses reference development services similar to:

```kotlin
issuerUrl = "https://ec.dev.issuer.eudiw.dev"
issuerUrl = "https://dev.issuer-backend.eudiw.dev"

override val walletProviderHost: String
    get() = "https://dev.wallet-provider.eudiw.dev"
```

The current `demo` flavor uses reference demo services similar to:

```kotlin
issuerUrl = "https://issuer.eudiw.dev"
issuerUrl = "https://issuer-backend.eudiw.dev"

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
* a wallet-provider service, or keep the hosted `walletProviderHost`

Then update the selected flavor's `WalletCoreConfigImpl.kt`. Keep the flavor's other settings
(for example `logoOrigins` and `withSupportedCredentialReusePolicies`), which the short example below
leaves out.

For an emulator, use `10.0.2.2` to reach services running on the host machine. Include the scheme
and port used by the local service:

```kotlin
override val issuersConfig: List<VciConfig>
    get() = listOf(
        VciConfig(
            issuerUrl = "https://10.0.2.2:8443",
            config = OpenId4VciManager.Config.Builder()
                .withClientAuthenticationType(
                    OpenId4VciManager.ClientAuthenticationType.AttestationBased(
                        clientId = "eudiw-abca"
                    )
                )
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

For a physical device, use the host computer's LAN IP address instead, and add that address (or the
development DNS name) as a `<domain>` in the debug network security config described below:

```kotlin
issuerUrl = "https://192.168.1.50:8443"
```

The issuer/verifier metadata, redirect URIs, and wallet deep links must match the app's configured
schemes and hosts. See [CONFIGURATION.md](CONFIGURATION.md) for those values.

`issuerUrl` must be exactly the `credential_issuer` value the local issuer publishes in its offers and
metadata, so configure the issuer with the address the device uses, for example
`https://10.0.2.2:8443`. The wallet refuses an offer from any other issuer, and refuses an offer passed
by reference unless it is hosted on that issuer's `https` origin.

The reference trust configuration also applies to local services. The issuer's metadata must be
signed with a certificate on the `wrpacProviders` list, and a PID issuer must be on `pidProviders`.
The hosted development lists do not contain your services, so point `loteLocations` at lists that do
(see [CONFIGURATION.md](CONFIGURATION.md#general-configuration)). With *Check Registration
Certificates* on, the issuer also needs a registration certificate on `wrprcProviders`.

The wallet opens a verifier's redirect only if it is an `https` URL, and loads issuer logos only over
`https`. A local verifier UI served over `http` is not returned to after a presentation.

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
trust anchors, verify signing and release checks, and follow [GO_LIVE.md](GO_LIVE.md).