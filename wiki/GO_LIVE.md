# EUDI Android Wallet Go-Live Guide

This guide explains how an integrator, implementer, or Member State team can take this Android
reference wallet and turn it into a production candidate.

It complements:

* [How to build](HOW_TO_BUILD.md)
* [How to configure the application](CONFIGURATION.md)
* [Theming and branding guide](THEMING.md)
* [Main README](../README.md)

The reference application is not production ready as delivered. It uses demo services, demo trust
anchors, broad custom-scheme deep links, source-level configuration, and security defaults that must
be reviewed for a high-assurance wallet. Treat this guide as a go-live work plan, not as a legal
certification or a replacement for a full security assessment.

## Audience

This guide is for technical teams that will operate or integrate a production wallet, including:

* Member State wallet teams.
* Wallet providers.
* Mobile application developers.
* Issuer, verifier, wallet-provider, and QTSP integration teams.
* Security, privacy, compliance, and accreditation teams.
* CI/CD and release engineers.

Readers should understand Android development, Kotlin, Gradle, OAuth/OpenID flows, TLS, and mobile
security basics.

## Production Principle

Do not publish the application by only switching to `demoRelease`.

Create a separate production configuration, replace every demo endpoint and certificate, perform a
MASVS-aligned assessment, add runtime hardening, and run a formal release process.

## Out Of Scope

This guide focuses on taking the Android reference wallet application to a production-ready mobile
deployment posture. It does not define every backend, governance, or product decision around a
national wallet ecosystem.

The following items are out of scope and must be designed, implemented, assessed, and approved by
the Member State or wallet provider:

* Enrollment and identity proofing flows before PID issuance.
* Selfie capture, video capture, liveness detection, document scanning, face matching, registry
  lookup, or any equivalent national onboarding process.
* The integration contract between the wallet app, the issuer, and any identity-proofing or
  population-register backend.
* Legal certification, accreditation, conformity assessment, supervisory approval, and governance
  decisions.
* Full production implementation of issuer, verifier, wallet-provider, QTSP, or status-list
  backend services.
* Non-Android form factors such as web wallets, browser extensions, desktop apps, kiosk apps, or
  iOS applications.
* Migration from an existing national wallet product unless the implementer designs and tests that
  migration explicitly.

If the Member State requires selfie, video, remote identity proofing, in-person proofing, national
eID login, or another enrollment method, integrate that flow through the Member State onboarding and
issuer architecture. The app must only receive the production credential offer, authorization
redirect, or issuer-specific entry point after the enrollment policy has been satisfied.

## Go-Live Checklist

Use this checklist before the first production release.

| Area                    | Required production outcome                                                                                                                                                                                                                                                   |
|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| App identity            | Final application ID, app name, icon, signing key, Play Console or alternative distribution identity are defined.                                                                                                                                                             |
| Branding and theme      | App name, launcher icon, in-app logos, light and dark color palette, splash screen, and any sub-SDK (RQES) theme are replaced with the production brand; no EUDI reference assets remain; text/background contrast meets accessibility targets. See [THEMING.md](THEMING.md). |
| Build variants          | A dedicated production flavor exists, for example `prodRelease`; `dev` and `demo` remain non-production only.                                                                                                                                                                 |
| Signing                 | Release keys are generated, stored in HSM/KMS or CI secret storage, rotated according to policy, and never committed.                                                                                                                                                         |
| Issuers                 | All OpenID4VCI issuer URLs point to production issuer services controlled or approved by the implementer.                                                                                                                                                                     |
| Wallet provider         | `walletProviderHost` points to the production Wallet Provider service and supports the expected attestation endpoints.                                                                                                                                                        |
| Trust anchors           | Demo and development certificates are replaced by production IACA/reader/verifier trust anchors, or by a production ETSI trusted-list (LoTE) source with the dev relaxations removed.                                                                                         |
| Registration            | Every production issuer and verifier presents a registration certificate that validates against the configured registration trusted list, every issuer is entitled for the provider role of what it offers, and every issuer's registered scope covers the documents it offers. Issuance is refused otherwise.                                  |
| RQES                    | QTSP, TSA, client ID, redirect URI, and certificate retrieval settings are production values.                                                                                                                                                                                 |
| Secrets                 | No production secret is hardcoded in Kotlin, Gradle, resources, or `BuildConfig`.                                                                                                                                                                                             |
| Network                 | Cleartext traffic is disabled; trust-all certificate logic is absent; TLS policy and certificate pinning strategy are agreed; the timeouts and response-size limit in `NetworkConfig` suit the production services.                                                        |
| Storage                 | Wallet data, database keys, PIN material, and logs are protected, excluded from backup, and migration-safe.                                                                                                                                                                   |
| Data deletion           | Removing a document deletes the app's records about it under the rules in [Data Deletion And Factory Reset](#data-deletion-and-factory-reset), and **Reset wallet** deletes the user's data and starts the app again at onboarding. Every local store added by the implementer is covered by both. |
| Authentication          | PIN, PIN strength rules, device screen-lock requirement, biometrics, device credential fallback, key authentication, lockout, and recovery policies are approved.                                                                                                            |
| Deep links              | All inbound URI schemes, hosts, and parameters are validated and threat-modeled; `rqesDocRetrievalHost` names the production document-retrieval host instead of `*`.                                                                                                                                                                                              |
| Presentation relay risk | Relay attack risk is documented, verifier compensating controls are defined, and residual risk is accepted for each presentation scenario.                                                                                                                                    |
| Logs                    | Production logs do not include PID, credentials, tokens, request objects, signatures, keys, or user decisions.                                                                                                                                                                |
| Screen capture          | `ConfigUILogic.shouldPreventScreenCapture` is true, so screenshots, screen recordings and the recent-apps thumbnail show no wallet content (the RQES SDK's screens excepted). See [Screenshots, Overlays, And Sensitive UI](#screenshots-overlays-and-sensitive-ui). |
| RASP                    | Play Integrity, commercial protection such as DexGuard, or a manual RASP strategy is implemented.                                                                                                                                                                             |
| MASVS                   | Controls are mapped to evidence and tested with static, dynamic, and manual security testing.                                                                                                                                                                                 |
| Privacy                 | DPIA, data minimization, retention, telemetry, consent, and privacy notice are complete.                                                                                                                                                                                      |
| Operations              | Monitoring, incident response, vulnerability disclosure, certificate rotation, and forced update processes exist.                                                                                                                                                             |

## Current Project Shape

The application is a modular Android project. Important modules for production are:

| Module                       | Production relevance                                                                                                                   |
|------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `app`                        | Application ID, signing, release build type, baseline profile packaging.                                                               |
| `assembly-logic`             | Android application class, manifest, permissions, activities, deep links, NFC service, app name.                                       |
| `core-logic`                 | Wallet Core integration, issuer configuration, wallet provider, trust stores, document issuance, presentation, revocation, reissuance. |
| `business-logic`             | Global app config, RQES config, logging, encrypted preference storage, crypto helpers.                                                 |
| `authentication-logic`       | PIN and biometric storage providers and controllers, PIN throttle/lockout, authentication configuration.                               |
| `storage-logic`              | SQLCipher-backed Room database and local transaction/revocation storage.                                                               |
| `network-logic`              | Ktor client, its timeouts and response-size limit (`NetworkConfig`), logging level, wallet attestation repository.                     |
| `resources-logic`            | Strings, images, raw certificate resources.                                                                                            |
| `analytics-logic`            | Optional analytics provider integration.                                                                                               |
| `build-logic`                | Gradle convention plugins, flavors, deep-link constants, lint, dependency check, managed devices.                                      |
| `baseline-profile`           | Baseline profile generation for release performance.                                                                                   |
| `test-logic`, `test-feature` | Test-only support modules for shared unit and instrumentation test utilities; not production runtime modules.                          |

Current flavors:

* `dev`: development environment values, app ID suffix `.dev`.
* `demo`: public demo environment values.

Current build types:

* `debug`: debuggable, no minification, verbose network logging.
* `release`: not debuggable, minification enabled, release signing applied.

Current generated variants:

* `devDebug`
* `devRelease`
* `demoDebug`
* `demoRelease`

For production, add and use a dedicated `prodRelease` variant.

## Source Of Truth Files

Keep these files under strict review:

| File                                                                                                      | Why it matters                                                                                       |
|-----------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| `build-logic/convention/src/main/kotlin/project/convention/logic/AppFlavor.kt`                            | Defines product flavors and application ID suffixes.                                                 |
| `app/build.gradle.kts`                                                                                    | Defines app ID, signing config, release minification, version code.                                  |
| `assembly-logic/build.gradle.kts`                                                                         | Defines app display name placeholder.                                                                |
| `assembly-logic/src/main/AndroidManifest.xml`                                                             | Defines exported components, permissions, backups, deep links, NFC service, network security config. |
| `core-logic/src/<flavor>/java/eu/europa/ec/corelogic/config/WalletCoreConfigImpl.kt`                      | Main wallet network, trust, issuer, document, and presentation configuration.                        |
| `business-logic/src/<flavor>/java/eu/europa/ec/businesslogic/config/ConfigLogicImpl.kt`                   | Flavor identity, changelog URL, app behavior toggles.                                                |
| `business-logic/src/<flavor>/java/eu/europa/ec/businesslogic/config/RQESConfigImpl.kt`                    | RQES QTSP/TSA/client config and document retrieval trust config.                                     |
| `network-logic/src/main/java/eu/europa/ec/networklogic/di/NetworkModule.kt`                               | Ktor client behavior: timeouts and response-size limit (`NetworkConfig`), and release logging.       |
| `network-logic/src/main/res/xml/network_security_config.xml`                                              | Cleartext and certificate trust policy.                                                              |
| `business-logic/src/main/res/xml/backup_rules.xml`                                                        | Auto Backup policy for Android 11 and below: every domain excluded.                                  |
| `business-logic/src/main/res/xml/data_extraction_rules.xml`                                               | Cloud backup and device transfer policy for Android 12 and above: every domain excluded from both.   |
| `authentication-logic/src/main/java/eu/europa/ec/authenticationlogic/storage/PrefsPinStorageProvider.kt`  | PIN hashing, salt, iteration count, constant-time comparison.                                        |
| `authentication-logic/src/main/java/eu/europa/ec/authenticationlogic/config/AuthenticationConfig.kt`      | PIN lockout policy, and the session lock timeouts.                                                   |
| `network-logic/src/main/java/eu/europa/ec/networklogic/config/NetworkConfig.kt`                         | HTTP client timeouts and the response-size limit.                                                    |
| `ui-logic/src/main/java/eu/europa/ec/uilogic/config/ConfigUILogicImpl.kt` | Login and protected-flow screen lists used by the session lock, the claims that hold a picture of the holder, and whether screen capture is prevented. |
| `authentication-logic/src/main/java/eu/europa/ec/authenticationlogic/storage/PrefsPinThrottleProvider.kt` | Persisted PIN failure counter, lockout level, and the lockout on the elapsed real time and boot.     |
| `business-logic/src/main/java/eu/europa/ec/businesslogic/controller/storage/PrefsController.kt`           | Encrypted DataStore and database key storage.                                                        |
| `storage-logic/src/main/java/eu/europa/ec/storagelogic/di/LogicStorageModule.kt`                          | SQLCipher database setup and migration behavior.                                                     |
| `resources-logic/src/main/java/eu/europa/ec/resourceslogic/theme/values/ThemeColors.kt`                   | Brand color palette (light and dark) plus extra semantic colors. See [THEMING.md](THEMING.md).       |
| `resources-logic/src/main/res/drawable/ic_logo_icon.xml`, `ic_logo_icon_and_text.xml`                     | In-app brand logos used on the splash and content headers.                                           |
| `resources-logic/src/<flavor>/res/mipmap-*`                                                               | Per-flavor launcher icons.                                                                           |

## Create A Production Flavor

Do not reuse `demo` for production. Add a new flavor, for example `prod`, so production values are
isolated and reviewable.

### 1. Add `Prod` To `AppFlavor`

> Note: there are **two** distinct `AppFlavor` enums in different modules — the Gradle one in
> `build-logic` (`Dev`, `Demo`; declares the product flavors) and a separate runtime one in
> `business-logic` (`ConfigLogic.kt`: `DEV`, `DEMO`; read by app code). Update **both** when adding a
> production flavor. This step covers the `build-logic` enum; [step 2](#2-add-production-config-source-sets)
> covers the `business-logic` enum.

Edit:

`build-logic/convention/src/main/kotlin/project/convention/logic/AppFlavor.kt`

Example:

```kotlin
enum class AppFlavor(
  val dimension: FlavorDimension,
  val applicationIdSuffix: String? = null,
  val applicationNameSuffix: String? = null
) {
  Dev(
    dimension = FlavorDimension.contentType,
    applicationIdSuffix = ".dev",
    applicationNameSuffix = " Dev"
  ),
  Demo(
    dimension = FlavorDimension.contentType,
    applicationIdSuffix = ".demo",
    applicationNameSuffix = " Demo"
  ),
  Prod(
    dimension = FlavorDimension.contentType
  )
}
```

Recommended production behavior:

* `Prod` should normally have no `applicationIdSuffix`.
* `Prod` should normally have no `applicationNameSuffix`.
* `Dev` and `Demo` should both have `applicationIdSuffix` values so they install as separate apps
  and cannot collide with the production app.
* `Dev` and `Demo` should both have visible `applicationNameSuffix` values so testers can clearly
  distinguish non-production builds from the production wallet.
* Do not publish a `dev` or `demo` variant to a production distribution channel.

After this change, the expected app identities are:

| Flavor | Example application ID behavior | Example app name behavior |
|--------|---------------------------------|---------------------------|
| `dev`  | `<baseApplicationId>.dev`       | `<appName> Dev`           |
| `demo` | `<baseApplicationId>.demo`      | `<appName> Demo`          |
| `prod` | `<baseApplicationId>`           | `<appName>`               |

### 2. Add Production Config Source Sets

Create these source-set files by copying the `demo` versions and replacing values:

```text
core-logic/src/prod/java/eu/europa/ec/corelogic/config/WalletCoreConfigImpl.kt
business-logic/src/prod/java/eu/europa/ec/businesslogic/config/ConfigLogicImpl.kt
business-logic/src/prod/java/eu/europa/ec/businesslogic/config/RQESConfigImpl.kt
```

In `ConfigLogicImpl`, set the production flavor explicitly:

```kotlin
override val appFlavor: AppFlavor
    get() = AppFlavor.PROD
```

Also update:

`business-logic/src/main/java/eu/europa/ec/businesslogic/config/ConfigLogic.kt`

Example:

```kotlin
enum class AppFlavor {
    DEV, DEMO, PROD
}
```

Then in production:

```kotlin
override val appFlavor: AppFlavor
    get() = AppFlavor.PROD
```

### 3. Build The Production Variant

After adding `prod`, expected variants include:

* `prodDebug`
* `prodRelease`

Build commands:

```powershell
.\gradlew.bat clean
.\gradlew.bat :app:assembleProdRelease
.\gradlew.bat :app:bundleProdRelease
```

Use Android App Bundle (`.aab`) for Google Play distribution unless your distribution channel
requires an APK.

## Application Identity

Production identity values must be final before external testing.

| Setting         | Current location                                                  | Production guidance                                                                                                                      |
|-----------------|-------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `applicationId` | `app/build.gradle.kts`                                            | Use a final reverse-DNS ID owned by the implementer, for example `eu.example.wallet`. Changing it after release creates a different app. |
| `namespace`     | `app/build.gradle.kts` and modules                                | Can remain internal package names, but align with your code ownership and policy.                                                        |
| App name        | `assembly-logic/build.gradle.kts`, manifest placeholder `appName` | Use the official wallet name approved for the Member State or wallet provider.                                                           |
| App icon        | `assembly-logic/src/main/AndroidManifest.xml`, mipmap resources   | Replace reference icons with production brand assets.                                                                                    |
| Version name    | `version.properties`                                              | Replace `yyyy.mm.v` with your release versioning scheme.                                                                                 |
| Version code    | `app/build.gradle.kts` or CI mutation                             | Must monotonically increase for app stores.                                                                                              |
| Min SDK         | `build-logic/.../KotlinAndroid.kt`                                | Current `minSdk` is 29. Confirm with your device support policy.                                                                         |
| Target SDK      | `build-logic/.../AndroidApplicationConventionPlugin.kt`           | Current `targetSdk` is 37. Keep current with Android policy.                                                                             |

Recommended:

* Freeze `applicationId` before beta testing with real users.
* Keep `dev` and `demo` app IDs separate with suffixes.
* Ensure app display names make non-production builds obvious.
* Maintain a release manifest showing version name, version code, Git commit, signing certificate
  fingerprint, and dependency versions.

## Branding And Theme

The reference app ships with EUDI visual identity; a production wallet must replace it. App name and
launcher icon are covered under [Application Identity](#application-identity). The remaining theme and
branding surfaces are documented in **[THEMING.md](THEMING.md)**.

Surfaces to review before release:

| Surface                        | Where                                                                                 | Notes                                                                                                        |
|--------------------------------|---------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| Color palette (light and dark) | `resources-logic/.../theme/values/ThemeColors.kt`                                     | Both palettes plus the extra `success`/`warning`/`pending`/`divider` roles. Verify contrast in both modes.   |
| Typography and fonts           | `resources-logic/src/main/res/font/` and `.../theme/values/ThemeTypography.kt`        | Replace the bundled Roboto fonts and the type scale if your brand requires it.                               |
| Shapes                         | `resources-logic/.../theme/values/ThemeShapes.kt`                                     | Corner radii.                                                                                                |
| In-app logos                   | `resources-logic/src/main/res/drawable/ic_logo_icon.xml`, `ic_logo_icon_and_text.xml` | Used on the splash and content headers; brand colors are baked into the vectors.                             |
| Splash screen                  | `resources-logic/src/main/res/values-v31/themes.xml` and the in-app `SplashScreen`    | System window splash plus the Compose splash logo.                                                           |
| System bars                    | `assembly-logic/.../ui/MainActivity.kt` (`enableEdgeToEdge`)                          | Edge-to-edge; controlled here, not by the XML theme.                                                         |
| RQES signing UI                | `business-logic/src/<flavor>/.../RQESConfigImpl.kt`                                   | The RQES SDK carries its own theme; a wallet rebrand does not restyle it unless you override `themeManager`. |

Requirements:

* No EUDI reference branding (name, icon, logos, colors, splash) remains in the production artifact.
* Light and dark palettes are both rebranded and meet WCAG AA contrast for text.
* The signing (RQES) flow either matches your brand or the SDK default is explicitly accepted.
* Run the verification steps and the production checklist in
  [THEMING.md](THEMING.md#production-and-accessibility-checklist).

## Release Signing

Current release signing is configured in:

`app/build.gradle.kts`

The reference configuration expects:

* Keystore file at the repo root as `sign`.
* Key alias from `local.properties` or `ANDROID_KEY_ALIAS`.
* Key password from `local.properties` or `ANDROID_KEY_PASSWORD`.
* Store password from the same `androidKeyPassword` / `ANDROID_KEY_PASSWORD` value in the current
  reference Gradle file.

Production requirements:

* Do not commit the keystore.
* Do not commit passwords, aliases, or local signing config.
* Do not store production signing material in a developer workstation as the only source.
* Prefer CI secret storage backed by HSM/KMS, Google Play App Signing, or an equivalent controlled process.
* Use separate passwords for keystore and key if your policy requires it.
* If using separate store and key passwords, change `app/build.gradle.kts` to read a distinct
  `ANDROID_KEYSTORE_PASSWORD` or equivalent CI secret for `storePassword`.
* Record the SHA-256 fingerprint of the app signing certificate.
* Restrict who can produce signed production artifacts.

Recommended Gradle shape:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("ANDROID_KEYSTORE_PATH"))
        keyAlias = System.getenv("ANDROID_KEY_ALIAS")
        keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
        storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
        enableV2Signing = true
        enableV3Signing = true
        enableV4Signing = true
    }
}
```

Notes:

* APK Signature Scheme v2+ is required for modern Android security. Use v3/v4 where supported by
  your tooling and distribution path.
* If using Google Play App Signing, protect both the upload key and the app signing key lifecycle.
* If using non-Play distribution, define your own key rotation and user migration process.

## CI/CD Release Pipeline

The repository already includes:

* OWASP Dependency Check plugin.
* Sonar plugin.
* Kover test coverage setup.
* GitHub workflow callers for dependency check, gitleaks, and Sonar.
* Fastlane lanes for tests, Firebase distribution, tagging, and GitHub releases.

For production, the minimum release pipeline should run:

```powershell
.\gradlew.bat clean
.\gradlew.bat lintProdRelease
.\gradlew.bat testProdReleaseUnitTest
.\gradlew.bat koverHtmlReportProdDebug
.\gradlew.bat dependencyCheckAnalyze
.\gradlew.bat :app:bundleProdRelease
```

Also add:

* Secret scanning on every branch and tag.
* SBOM generation.
* Dependency license review.
* Static application security testing.
* Malware scan of final artifacts.
* Verification that final artifact is not debuggable.
* Verification that final artifact uses the expected signing certificate.
* Verification that demo endpoints are absent from the final artifact.

Example final-artifact checks:

```powershell
.\gradlew.bat :app:processProdReleaseMainManifest
.\gradlew.bat :app:dependencies --configuration prodReleaseRuntimeClasspath
```

Inspect the merged manifest and dependency output as release evidence.

## Dependency Management

Use `gradle/libs.versions.toml` as the single source of truth for dependency and plugin versions.
Do not duplicate the active dependency versions in this guide.

Production rules:

* Use released versions, not snapshots, unless formally approved.
* Subscribe to security advisories for Wallet Core, RQES SDK, AndroidX, Ktor, Kotlin, SQLCipher,
  Tink, Koin, and Gradle plugins.
* Keep a dependency update SLA.
* Generate and archive an SBOM for every release.
* Keep `google()` and `mavenCentral()` as primary repositories.
* Avoid `mavenLocal()` in production CI builds because it can make builds non-reproducible.
* Only keep JitPack if every dependency from it is approved and pinned.

## WalletCoreConfig Overview

The most important production file is:

```text
core-logic/src/prod/java/eu/europa/ec/corelogic/config/WalletCoreConfigImpl.kt
```

It implements:

```kotlin
interface WalletCoreConfig {
    val config: EudiWalletConfig
    val issuersConfig: List<VciConfig>
    val documentCategories: DocumentCategories
    val revocationInterval: Duration
    val documentIssuanceConfig: DocumentIssuanceConfig
    val walletProviderHost: String
    val trustMarkSource: TrustMarkSource
    val isRegistrationCheckEnabled: Boolean
    val workerStopTimeout: Duration
}
```

Each property must be reviewed, including inherited defaults on `WalletCoreConfig`.
For Trust Mark settings, see
[Trust Mark configuration](CONFIGURATION.md#trust-mark-configuration).

## `EudiWalletConfig`

Current flavor implementations configure:

* Document key creation.
* OpenID4VP.
* Digital Credential API.
* Reader trust store (ETSI trusted lists in both reference flavors).
* ETSI trusted-list (LoTE) trust: one trust source shared by issuer trust, status-list signer
  trust, and reader authentication.
* Wallet Core log level (`configureLogging`: `LEVEL_DEBUG` for debug builds, `LEVEL_INFO` for release;
  see [Diagnostic logging](#diagnostic-logging)).

### Document Key Creation

Current code:

```kotlin
configureDocumentKeyCreation(
    userAuthenticationRequired = false,
    userAuthenticationTimeout = 30.seconds,
    useStrongBoxForKeys = true
)
```

This is the reference configuration as shipped. It is not a production recommendation for LoA High
PID or other high-assurance credentials.

Meaning:

| Value | Meaning | Production guidance |
| --- | --- | --- |
| `userAuthenticationRequired` | Whether document private keys require device user authentication before use. | For LoA High PID and other high-assurance EAA/QEAA credentials, set this to `true` unless an approved remote high-assurance key protection design replaces local key use. If `false`, the keys can be used without user authentication. The reference app asks for the PIN or a biometric before every credential release, but that prompt is a screen in the app, not a condition on the keys: a route to the send step that skipped the screen would release credentials without it. See [Authentication And Biometrics](#authentication-and-biometrics). |
| `userAuthenticationTimeout` | Time window after authentication during which key use remains allowed. `0.seconds` means authentication is required for every key use. | Use the shortest approved value that still supports the intended flow. `0.seconds` is appropriate for isolated high-risk operations where one prompt per key use is acceptable. For batch issuing, a very short window such as `10.seconds` may be needed so one strong authentication can cover the batch without repeatedly prompting the user. |
| `useStrongBoxForKeys` | Requests StrongBox-backed key storage where available. | Keep `true` for supported devices. Define fallback behavior for devices without StrongBox. Decide whether such devices are allowed, restricted, or rejected. |

Recommended production example for batch issuance:

```kotlin
configureDocumentKeyCreation(
    userAuthenticationRequired = true,
    userAuthenticationTimeout = 10.seconds,
    useStrongBoxForKeys = true
)
```

Recommended production example for strict per-use authentication:

```kotlin
configureDocumentKeyCreation(
    userAuthenticationRequired = true,
    userAuthenticationTimeout = 0.seconds,
    useStrongBoxForKeys = true
)
```

Requirements and decision points:

* For LoA High PID and other high-assurance EAA/QEAA credentials, document keys must be protected by
  strong user authentication and hardware-backed key protection, either locally on the device or
  through an approved remote high-assurance hardware-backed storage design.
* Local key use should be bound to strong authentication such as biometric authentication or device
  credential according to the approved assurance policy.
* Batch issuing and one-time-use credential issuance should be tested with the selected timeout. If
  `0.seconds` forces repeated prompts during one PID issuance flow, use a short approved timeout such
  as `10.seconds`, document the risk decision, and verify that the window is not usable outside the
  intended flow.
* If the supported device population lacks StrongBox, define a policy for hardware-backed TEE keys
  versus StrongBox-only devices.

### Key Storage Extension Points

The default Wallet Core configuration uses Android Keystore-backed secure areas for document keys.
Production teams must decide whether the default local storage model is sufficient or whether a
custom secure area, remote signing service, or external protection SDK is required.

Document key creation policy is configured in:

```text
core-logic/src/<flavor>/java/eu/europa/ec/corelogic/config/WalletCoreConfigImpl.kt
```

using:

```kotlin
configureDocumentKeyCreation(
    userAuthenticationRequired = true,
    userAuthenticationTimeout = 10.seconds,
    useStrongBoxForKeys = true
)
```

The default document key storage is selected by Wallet Core when `EudiWallet` is built. In this
application, the integration point is:

```text
core-logic/src/main/java/eu/europa/ec/corelogic/di/LogicCoreModule.kt
```

inside `provideEudiWallet(...)`:

```kotlin
val eudiWallet = EudiWallet(
    context = context,
    config = walletCoreConfig.config,
    walletProvider = walletCoreAttestationProvider,
    trustMarkSource = walletCoreConfig.trustMarkSource,
) {
    withLogger(walletCoreLogController)
    withTransactionLogger(walletCoreTransactionLogController)
    withKtorHttpClientFactory { httpClient }
    withDCAPIRegistration(walletCoreDCAPIRegistrationController)
}
eudiWallet.addTransferEventListener(walletCoreTransactionLogController)
```

If production requires document key storage other than the default Android Keystore secure area,
customize the `EudiWallet.Builder` block. Wallet Core exposes the following extension points:

| Extension point | Purpose | When to use |
| --- | --- | --- |
| `withSecureAreas(...)` | Supplies custom `org.multipaz.securearea.SecureArea` implementations for document key management. | Use when document keys must be created in a different secure area, external SDK, remote-backed secure area, or policy-controlled key provider. |
| `withStorage(...)` | Supplies custom document metadata/storage backing. | Use when the default Android storage path does not meet backup, migration, encryption, or operational requirements. |
| `withDocumentManager(...)` | Replaces the Wallet Core document manager. | Use only for advanced integrations that need full control over document lifecycle, storage, and key binding. |
| `withWalletKeyManager(...)` | Replaces the key manager used for wallet attestation/client-attestation keys. | Use when wallet attestation keys must be stored, attested, or unlocked through a custom secure area or remote high-assurance key service. |
| `DPopConfig.Custom(...)` in issuer configuration | Customizes OpenID4VCI DPoP key storage and unlock behavior. | Use when issuance proof-of-possession keys must be generated in a specific secure area, require user authentication, or satisfy issuer policy beyond `DPopConfig.Default`. |
| `withPresentationManager(...)` | Replaces the default remote/proximity presentation manager. | Use only if the default OpenID4VP/proximity implementation does not meet production key-handling, transaction logging, or protocol policy. A presentation manager supplied this way is not wrapped with Wallet Core's `PresentationLogger`: presentations then leave no transaction-history entry, and the app records no registration outcome, unless your implementation logs them. |

Production implementation rules:

* Treat document keys, wallet attestation keys, DPoP keys, and remote presentation protocol keys as
  separate key classes with separate policies.
* Document keys for LoA High PID and high-assurance EAA/QEAA credentials must be hardware-backed or
  protected by an approved remote high-assurance hardware-backed design.
* Wallet attestation keys must be generated and attested according to the Wallet Provider policy.
  If Android platform key attestation is required, the key manager and Wallet Provider endpoint must
  both support that policy.
* DPoP keys protect issuance access tokens against replay. Do not disable DPoP in production unless
  the issuer explicitly does not support it and the risk is accepted.
* Remote presentation uses protocol-level ephemeral key material for encrypted OpenID4VP responses.
  Ephemeral key material must be generated per transaction, must not be reused across verifiers, and
  must not be persisted longer than the protocol flow requires. If the selected Wallet Core version
  exposes a dedicated configuration option for this ephemeral key storage, configure it in the
  production `EudiWallet` or presentation-manager integration and record the exact SDK API in the
  release evidence.
* Test custom storage with device lock changes, biometric enrollment changes, app upgrade, app
  reinstall, backup/restore attempts, low-storage conditions, and issuer reissuance.

### OpenID4VP Configuration

Current code:

```kotlin
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
    withTransactionDataTypes(
        TransactionDataType.QES_APPROVAL,
        TransactionDataType.QES
    )
}
```

Production meaning:

| Setting | Meaning | What to put in production |
| --- | --- | --- |
| `ClientIdScheme.X509SanDns` | Verifier client identity is bound to a DNS name in an X.509 certificate. | Use when verifier certificates and trust anchors are managed and audited. |
| `ClientIdScheme.X509Hash` | Verifier identity is bound to a certificate hash. | Use when the verifier ecosystem requires hash-based certificate binding. |
| `ClientIdScheme.Preregistered` | Verifiers are explicitly configured in the wallet. | Use for closed pilots or controlled ecosystems. Add production verifier API URL, legal name, and client ID. |
| `withSchemes` | URI schemes the app accepts for OpenID4VP. | Keep only schemes required by your supported protocols and profiles. |
| `withFormats` | Credential formats and algorithms supported in presentation. | Keep only formats and algorithms your issuers and verifiers support and that are approved by your security profile. |
| `withTransactionDataTypes` | Signing transaction-data types accepted with a presentation request. | Keep QES approval and QES request enabled when supporting these flows; see [Transaction data configuration](CONFIGURATION.md#transaction-data-configuration). |

If using preregistered verifiers, add:

```kotlin
configureOpenId4Vp {
    withClientIdSchemes(
        listOf(
            ClientIdScheme.Preregistered(
                preregisteredVerifiers = listOf(
                    PreregisteredVerifier(
                        clientId = "my-production-verifier-client-id",
                        verifierApi = "https://verifier.example.eu",
                        legalName = "Example Member State Verifier"
                    )
                )
            )
        )
    )
}
```

Rules:

* `verifierApi` must be HTTPS.
* `legalName` must be the official entity name shown to users.
* `clientId` must match the verifier registration and protocol profile.
* Do not include development verifier URLs in production.
* If a verifier is not trusted, the user interface must clearly show that status before disclosure.

### Transaction Data

A presentation request can carry signing transaction data (`QES_APPROVAL` and `QES`). The consent
screen shows it in a **Data to be signed** section, open by default. The section lists every field
of the transaction data, since Wallet Core binds the transaction data as received into the
response; a QES approval, for example, returns a hash of the whole object. The signed attributes,
which the relying party chooses freely, are marked "Chosen by the relying party; not checked by
the wallet". A document the request carries inline is shown as included, with its type and size;
its content is not displayed. A document given by reference can be opened in the browser.

The wallet declines a request whose transaction data it cannot show in full, rather than show it
in part (`PresentationRequestInteractorImpl.canShowTransactionData`). That covers an entry of a
configured type the app cannot display, which Wallet Core passes through unparsed, and an entry
bound to a match the consent screen does not show, such as one for a revoked document. Each
offered option is checked against its own transaction data; an option with no document that can
be shown is not offered and not checked. The request screen then shows "This request asks you to
sign data that the wallet cannot show in full, so it was declined." in place of the documents,
with Share disabled and no Retry (`RequestDataUi.Declined`). The verifier receives no error
response. iOS behaves the same way.

The wallet displays hashes and checksums as the relying party supplied them. It does not verify
them: neither the app nor Wallet Core compares a document opened from its link with them, so the
screen cannot show that a hash belongs to that document. **Share** does not depend on the section
having been read.

The transaction log keeps the transaction data as received, decoded but otherwise unchanged,
including one-time passwords for document access and the whole content of a document carried
inline, so a large document makes a large entry. Transaction history shows the same fields
without the one-time passwords and without the open action. Decide whether one-time passwords
and inline documents belong in the log at all for your deployment.

### Relay Attack Risk In Presentation Flows

Production presentation flows must explicitly account for relay attacks. This is especially
important for high-assurance identity verification and for selective-disclosure-only requests such
as age checks.

The wallet's presentation flows implement standard session binding, such as OpenID4VP `nonce`,
`state`, `client_id` binding, and ISO 18013-5 session transcript binding. These controls are
necessary and protect against replay, injection, session mix-up, and similar protocol attacks when
verifiers validate them correctly. They do not, by themselves, prove that the wallet holder is
physically present at the verifier or prevent a live verifier request from being relayed to a
different legitimate wallet holder.

Relay is different from replay:

| Attack | What happens | Why standard binding is not enough |
| --- | --- | --- |
| Replay | An attacker reuses an old presentation response. | Fresh nonces, transaction binding, audience/client binding, and session transcript validation are intended to make old responses invalid. |
| Relay | An attacker forwards a live verifier request to another wallet holder, who creates a fresh, cryptographically valid response that is relayed back to the verifier. | The response is fresh and correctly bound to the verifier request, so cryptographic replay checks can pass even though the person interacting with the verifier is not the wallet holder. |

This is an architectural and ecosystem risk, not necessarily a wallet code bug. The wallet may be
fully conformant with OpenID4VP and ISO 18013-5 and still require verifier-side, issuer-side, or
deployment-policy controls to mitigate relay risk.

Verifier guidance:

* Document relay risk in integrator and relying-party guidance. Verifiers must understand that
  selective-disclosure-only requests, for example `age_over_18` without portrait or another
  holder-presence check, are vulnerable to live relay.
* For attended in-person verification where the relying party must know that the person in front of
  them is the wallet holder, consider requiring portrait disclosure or another approved biometric
  holder-binding claim. Human comparison of the live person to the disclosed portrait is the primary
  practical relay defense in this scenario.
* Requesting portrait or other biometric data must be lawful, proportionate, purpose-specific, and
  reflected in the verifier privacy notice. Do not request portrait for low-risk age checks unless
  the assurance requirement justifies the additional disclosure.
* For unattended or automated verification, do not treat a cryptographically valid age-only or
  claim-only response as proof of physical presence. Consider additional binding, such as
  server-validated app/device attestation, account/session risk signals, step-up authentication,
  lawful and consented location evidence, or other trust-framework-approved controls.
* For ISO 18013-5 proximity use cases where physical presence is expected, evaluate distance
  bounding or other proximity verification mechanisms. QR, BLE, or NFC engagement alone should not
  be documented as a complete relay defense unless the deployment profile includes a tested
  relay-resistant mechanism.
* Treat timing and latency checks as weak signals. They can help identify suspicious relays but do
  not prevent a fast relay and should not be the only control.

Wallet and protocol-profile guidance:

* Keep OpenID4VP replay/session-binding checks enabled and test that verifiers reject wrong or reused
  `nonce`, `state`, `client_id`, and response/session values.
* If the ecosystem profile allows wallet-side timestamps in VP responses or response envelopes, make
  the timestamp integrity-protected and bound to the transaction. Verifiers should reject responses
  outside a short freshness window or responses with suspicious end-to-end latency.
* Do not add unsigned or unauthenticated timestamps and treat them as a security control. Unsigned
  metadata can be altered by an attacker.
* If device attestation, location evidence, or app integrity is used as a compensating control, bind
  the evidence to a fresh verifier/backend nonce and to the same presentation transaction.
* Make the user consent screen clear about the verifier, the requested claims, and whether the flow
  is remote or proximity/in-person. Clear UX does not prevent relay by itself, but it reduces social
  engineering opportunities.
* The consent screen marks a request for a picture of the holder. When the selected option includes
  a claim listed in `ConfigUILogic.userImageClaimKeys` (`portrait` and `picture` in the reference
  configuration), the screen shows a biometric-data notice, and **Share** asks for confirmation
  whenever the picture would be disclosed. The picture is still selected like any other requested
  claim. Add to that list any other claim in your credentials that holds a picture of the holder;
  the same list decides which claims are displayed as images. Other biometric claims, such as the mDL's `biometric_template_*`
  elements, are not covered; extend the check if your credentials carry them.

Production decision record:

| Scenario | Required decision |
| --- | --- |
| Age-only remote verification | Decide whether relay risk is acceptable. If not, define additional controls or require a different presentation policy. |
| In-person age or identity verification | Decide whether portrait disclosure and human comparison are mandatory. |
| ISO 18013-5 proximity verification | Decide whether distance bounding, latency checks, human portrait comparison, or another proximity control is required. |
| Automated high-value verification | Define additional binding, such as app/device attestation, account risk, location evidence, or transaction monitoring. |
| Verifier certification/onboarding | Require verifiers to document how they mitigate relay for each requested claim set and relying-party context. |

Evidence to collect:

* Threat model section distinguishing replay, session fixation, phishing, and live relay.
* Verifier policy for claim-only, age-only, portrait, and proximity requests.
* Test evidence for relayed QR/OpenID4VP requests and relayed proximity engagement where feasible.
* Residual-risk sign-off for scenarios where relay cannot be fully mitigated.
* User-facing and verifier-facing guidance explaining the limits of selective disclosure for
  physical-presence assurance.

### Digital Credential API

Current code (both flavors):

```kotlin
configureDCAPI {
    withEnabled(true)
    withSupportedProtocols(
        DCAPIProtocol.ISO_MDOC,
        DCAPIProtocol.OPENID4VP_V1_SIGNED
    )
}
```

Production guidance:

* Keep enabled only if your product supports Android Digital Credential API flows.
* When enabled, at least one supported protocol is mandatory — `DCAPIConfig` fails at build time
  otherwise. A request for any protocol not listed is rejected.
* Enable only the protocols approved by your ecosystem profile. Do not enable
  `OPENID4VP_V1_UNSIGNED` unless unsigned (unauthenticated) OpenID4VP requests over the DC API are
  explicitly acceptable.
* Users can turn the API off in Settings ("Offer documents to apps and websites"). It is on by
  default. While it is off, the wallet removes its documents from the Android credential registry
  and declines any request that still arrives through the API; turning it back on registers the
  documents again. If the system registry cannot be cleared, for example on a device without
  Google Play services, the entries may remain until a later update succeeds, but requests are
  still declined. The switch is shown only when the build enables the API. HLR.48 and IA.30 ask
  for this control in the user's hands, so keep it in production builds; the build setting alone
  does not meet them.
* A document the revocation check has blocked (revoked, suspended or unrecognised) is left out of
  the registry, so it is not offered to other apps. At the end of each run the revocation check
  updates the registry when the blocked documents differ from those it last left out
  (`WalletCoreDCAPIRegistrationController.excludedDocumentIds`,
  `DocumentManagerWithoutExcluded`). If the revocation records cannot be read, an update clears the
  registry instead.
* Test with all supported Android versions and browsers.
* Validate all inbound DC API intents.
* Confirm policy implications for relying parties and browser mediation.
* Include DC API flows in MASVS platform-interaction testing.

### Reader Trust Store

Both reference flavors derive reader trust from ETSI trusted lists — see
[ETSI Trusted Lists (LoTE)](#etsi-trusted-lists-lote) below. The Wallet Core SDK also supports a
static trust store built from PEM files under:

```text
resources-logic/src/main/res/raw
```

Use the configured ETSI trusted lists or supply explicit trust anchors for reader authentication.
Explicit `trustedCertificates(...)` take precedence over the shared trusted-list configuration for
reader authentication.

Reader anchors do not vouch for registration certificates. Without `configureEtsiTrust`, supply
`configureWrpRegistrationTrust(...)` and `configureIssuerRegistrationTrust(...)`: with the registration
check on, Wallet Core 0.31.1 refuses to build `EudiWallet` (`IllegalArgumentException`) when nothing
vouches for registration certificates (for issuers, when signed metadata is required).

The repository still ships development/demo trust-anchor resources, although the reference
flavors no longer load them for reader trust. If your deployment uses the static model, in
production:

* Remove trust anchors that are not part of the production trust framework.
* Add production IACA, reader root, verifier, or scheme certificates according to your trust model.
* Give files clear names, for example `ms_iaca_2026.pem`.
* Document certificate owner, purpose, fingerprint, validity, and rotation plan.
* Add tests that verify expected production trust anchors are present and demo anchors are absent.

Example:

```kotlin
configureReaderAuthentication {
    trustedCertificates(
        context,
        R.raw.ms_iaca_2026,
        R.raw.ms_reader_root_2026
    )
    enforceIfPresent()
}
```

Certificate governance:

* Store source PEMs in version control only if policy allows public trust anchors there.
* Do not store private keys in the app repository.
* Define rotation before expiry.
* Define emergency distrust and app update procedures.

### ETSI Trusted Lists (LoTE)

Both reference flavors (`dev` and `demo`) configure ETSI TS 119 602 trusted lists in
`WalletCoreConfigImpl.kt`. Their credential issuer-trust policy uses `ENFORCE` for the configured
mdoc and SD-JWT PID types and defaults to `INFORM` for other attestations. The Wallet classifies
only those PID types. See [CONFIGURATION.md](CONFIGURATION.md) for the reference configuration
and its expected behavior.

For a production deployment that requires `ENFORCE` for every issued credential type, the example
below uses global enforcement and requires corresponding approved trust classifications:

```kotlin
configureEtsiTrust {
  loteLocations(
    SupportedLists(
      /* production LoTE URLs */
    )
  )
  classifications(
    AttestationClassifications(
      /* credential-type → context mapping */
    )
  )
}
configureIssuerTrust {
  policy {
    default(TrustPolicy.Action.ENFORCE)
  }
  requireSignedMetadata()
  // the production target: registration checking on. The reference build instead drives both
  // policies from the Settings switch, which ships off — see the next section
  configureIssuerRegistrationPolicy(IssuerRegistrationPolicy.Enabled)
}
configureDocumentStatusResolver {
  configureTrust {
    policy {
      default(TrustPolicy.Action.ENFORCE)
    }
  }
}
configureReaderAuthentication {
  enforceIfPresent()
}
configureWrpRegistrationPolicy(WrpRegistrationPolicy.Enabled)
```

Validate the chosen configuration with the issuance and presentation services supported by your
deployment. The example enforces status-list trust for every type, which needs every type you
check to be classified: with the reference classification, which covers only the PID, a global
`ENFORCE` makes the status check of every other document fail.

`requireSignedMetadata()` applies whether registration checking is on or off. The wallet refuses an
issuer that does not serve signed metadata, or whose metadata signing certificate does not
validate against the access-certificate list (`wrpacProviders`). Wallet Core checks this when an
offer is resolved, when wallet-initiated issuance or re-issuance starts, and when the app reads an
issuer's metadata. Registration checking adds the issuer's registration certificate, which needs
the signed metadata, and, for wallet-initiated issuance and re-issuance, builds the issuance itself
from the signed metadata. With it off, wallet-initiated issuance and re-issuance check the signed
metadata and then issue from the unsigned metadata the issuer also serves; an offer always issues
from the signed metadata it was resolved with.

Production requirements for a trusted-list deployment:

* Use production LoTE URLs from the approved trust framework, over HTTPS.
* Remove `relaxCertificateProfiles()` and `pkixRevocationPolicy(PkixRevocationPolicy.Disabled)`. The first removes the end-entity
  certificate profile checks (ETSI TS 119 412-6 for PID and wallet-provider signing certificates,
  TS 119 411-8 for access certificates) from every list. The second turns off CRL and OCSP checking during chain
  validation; a certificate a list names directly is not checked for revocation in either case. Both
  are workarounds for development PKI and must not ship. Without the call, Wallet Core 0.31.1 uses
  `PkixRevocationPolicy.CRLPriority`: the CRL first, OCSP only when the CRL cannot be fetched, and the
  chain fails when neither answers. Do not ship `SoftFail` either: it checks the CRL only and accepts a
  chain whose CRL cannot be fetched.
* Revocation information must be served over HTTPS. The wallet uses HTTPS only, and revocation is
  no exception: CRL and OCSP downloads over `http://` are refused like any other cleartext request.
  Every certificate below a list entry, in every chain the wallet validates against the trusted
  lists, must give its CRL distribution points and OCSP address as `https://` URLs. A certificate
  that gives only `http://` addresses, which is common, always fails. This is a requirement on the
  trust framework's certificate authorities; see
  [Production Backend Requirements](#production-backend-requirements).
* Classify every credential type you issue or check the status of (`classifications`), and decide
  how the classifications will be maintained. The reference flavors classify only the PID.
  Classification decides what is evaluated. Under `ENFORCE`, issuance of an unclassified type is
  refused and its status checks fail. Under `INFORM`, an unclassified type is not evaluated, and the
  app shows its issuer as not verified.
* Configure the registration-certificate list (`wrprcProviders`) and enable registration checking
  as described in the next section.
* Issuer trust: `ENFORCE` refuses a credential whose issuer is not trusted or cannot be evaluated;
  Wallet Core does not store it and reports `DocumentFailed`. `INFORM` stores the credential and
  reports the verdict with it (`issuerTrustResult`). The reference flavors enforce for the PID and
  inform for everything else.
* Status-list trust: Wallet Core checks a status list's signature against the certificate in the
  list's own header, so only the trust evaluation ties the list to its issuer. `ENFORCE` rejects a
  list whose signer is not trusted; the status check then fails, the app records a failed refresh,
  and the document keeps its last known state. `INFORM` accepts the list whatever its signer and
  only logs the verdict. Use `ENFORCE` in production; a rule for one type uses the status context,
  for example `forContext(VerificationContext.PIDStatus, TrustPolicy.Action.ENFORCE)`. This choice
  answers IA.19 on the authenticity of revocation information, as the audit maps it.
* Reader authentication: with `enforceIfPresent()`, the reference setting, a proximity or Digital
  Credential API mdoc reader that sends no reader authentication receives the data once the user
  accepts the warning on the consent screen. A reader whose authentication fails verification
  receives an empty response with status 10, even if the user shares. `alwaysRequire()` gives that
  empty response to both. Remote OpenID4VP behaves the same under either setting: with the
  `X509SanDns` and `X509Hash` client-ID schemes a request must be signed, and an untrusted one is
  rejected when it is resolved. This choice answers HLR.32 and IA.22, which require the relying party to be
  authenticated before attributes are released; record the decision if unauthenticated readers are
  accepted.
* Trusted-list signatures: the default verifier checks each list's JWS signature with the key of the
  first certificate in its `x5c` header, without validating that certificate's chain, revocation or
  profile. If the trust framework requires the list signer to be pinned or chain-validated, supply a
  verifier with `jwtSignatureVerifier(...)` in `configureEtsiTrust`.
* Caches: a downloaded list is reused from disk until its own `nextUpdate`; `fileCacheExpiration`
  (24 hours) applies only when the list gives no future `nextUpdate`. Trust anchors are kept in
  memory for 20 minutes (`cacheTtl`), and refreshing them rereads the disk cache, so a removal from
  a list takes effect only when the disk entry expires. The reference flavors keep both defaults.
  Set them according to how quickly a removal must take effect.
* Under `INFORM` a document is stored even when its issuer is not trusted. The app keeps Wallet
  Core's verdict for each document (`issuerTrustResult` on the issuance event, stored in the
  `issuerTrustRecords` table). Document details say "Issuer verified" only when the chain was
  trusted; on the issuance success screen the issuer gets the verified badge only when every issued
  document's issuer was trusted. A document whose issuer was untrusted, or not evaluated (in the
  reference configuration, every non-PID document), says "Issuer not verified" on both screens.
  The verdict is the one taken at issuance and is not re-evaluated later, for example when an
  issuer leaves the trusted list.
* Test both directions, for every protocol and for PID and non-PID documents: a party on the list
  succeeds; one not on the list is refused, or warned about as described in the next section. Cover
  trusted, untrusted and unverifiable issuers, status lists and readers.

### Registration Certificates: The Second Trust Layer

Trust is evaluated on two independent layers, and passing one says nothing about the other:

| | Access certificate | Registration certificate |
|---|---|---|
| Answers | *who is this party* | *what is it registered to do* |
| Establishes | authentication of the issuer or verifier | the registered identity, declared purpose, privacy policy, the provider role the party is entitled to, and the attestation types and claims it may issue or request |
| Trust source | its own trusted list | the registration-certificate trusted list, from the same LoTE configuration, or the anchors given to `configureWrpRegistrationTrust` / `configureIssuerRegistrationTrust`, which take precedence |

The two layers are not interchangeable: an authenticated issuer that is not registered for what it
offers is refused, and a registered issuer that fails authentication is refused too.

**This layer is off by default and must be switched on.** A relying party's access-certificate
trust is enforced where the protocol and trust policy allow it (where it does not, the user is
warned before sharing; see below), and an issuer is shown as verified only when Wallet Core
reports it trusted, so one accepted under `INFORM` without that verdict is shown as not verified,
but registration-certificate checking is governed by the *Check Registration Certificates*
setting, which the reference build ships **off**. One setting drives both the issuer policy
(`configureIssuerRegistrationPolicy`) and the verifier policy (`configureWrpRegistrationPolicy`).
While it is off, nothing is evaluated on registration grounds: nothing is refused or warned about
over a registration, and the verified indicator falls back to the access certificate alone.
**A production deployment must turn it on** — and should decide whether to keep the switch
reachable by users at all, since it turns a trust check off. Wallet Core reads the setting when it
creates its managers, so a change, including the return to off by **Reset wallet**, takes effect
at the next app start.

Everything below describes behavior with the setting on, except where noted. The app treats the
two directions differently, and the asymmetry is intentional:

* **Issuance is strict.** A document is stored only when the issuer's registration is verified and
  covers the credential being issued. Anything short of that — no registration, one that does not
  pass validation, one not entitled to the provider role the offer requires, or one that does not
  cover the offer — stops the flow and reports it on the same
  "Issuance blocked" sheet an authentication failure uses, because the distinction does not change
  what the user can do about it. The refusal lands before anything is stored, and a refused
  re-issuance leaves the document the user already holds in place.
* **Presentation warns rather than blocks.** A request whose registration is not verified still
  reaches the consent screen, shown without the verified indicator and behind a warning the user
  must acknowledge before sharing. A request whose registration *is* verified but that reaches
  beyond the registered scope keeps the indicator, marks each requested item outside that scope,
  and gates sharing behind the same acknowledgement. Access-certificate failures continue to block
  outright where the protocol allows it. Where they do not (an mdoc reader, in proximity or over the
  DC-API, that sends no reader authentication or one that does not verify), the request reaches the
  consent screen behind its own warning that the requester's identity could not be verified, with
  the same acknowledgement before sharing. This warning does not depend on the registration
  setting. A reader whose authentication fails verification still receives an empty response
  (status 10) after the user shares; see Reader authentication under
  [ETSI Trusted Lists (LoTE)](#etsi-trusted-lists-lote).

The verified indicator needs both layers: a trusted access certificate and a registration that did
not fail validation.

Production requirements:

* **Turn the registration check on**, and decide whether the switch stays user-visible. Shipping it
  on but toggleable leaves users able to disable a trust check; shipping it on and hidden — or
  fixing both policies to `Enabled` in your production flavor — is the stricter choice.
* Configure the registration-certificate trusted list (`wrprcProviders`) alongside the others.
  Without it, registration certificates have no trust source, and issuance is refused rather than
  waved through.
* Confirm with every issuer that it presents a registration certificate the configured trusted list
  accepts, that it carries the entitlement for the provider role its offers require — `PID_Provider`
  for PIDs, and one of the QEAA, PuB-EAA or EAA provider entitlements for anything else — and that
  its registered scope covers everything it offers. This is a hard dependency: an issuer that fails
  any of these conditions cannot deliver documents at all.
* Confirm with every verifier that its requests carry a registration certificate and stay within
  the registered scope, so users are not asked to acknowledge a warning on every routine
  transaction.
* Confirm that each party's registration and access certificates are issued to the same registered
  organization, and that the registration certificate's revocation status can be checked. A
  registration whose status cannot be established does not pass.
* Test the refusals, not just the happy path: an unregistered issuer, an issuer whose registration
  lacks the entitlement for a type it offers, an issuer offering a type outside its registered
  scope, and a verifier over-asking. Confirm a refused re-issuance leaves the existing document in
  place and usable.
* Test both states of the setting if you keep it toggleable, including applying changes after an
  app restart.

## Issuer Configuration: `issuersConfig`

Current demo code contains issuer URLs such as:

```kotlin
issuerUrl = "https://issuer.eudiw.dev"
issuerUrl = "https://issuer-backend.eudiw.dev"
```

Production values must point to your production OpenID4VCI issuers.

Example:

```kotlin
override val issuersConfig: List<VciConfig>
    get() = listOf(
        VciConfig(
            issuerUrl = "https://issuer.pid.example.eu",
            config = OpenId4VciManager.Config.Builder()
                .withClientAuthenticationType(
                    OpenId4VciManager.ClientAuthenticationType.AttestationBased(
                        clientId = "your-wallet-client-id"
                    )
                )
                .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
                .withParUsage(OpenId4VciManager.Config.ParUsage.IF_SUPPORTED)
                .withDPopConfig(DPopConfig.Default)
                .build(),
            order = 0
        )
    )
```

| Field | Meaning | Production value |
| --- | --- | --- |
| `issuerUrl` | Base URL of an OpenID4VCI credential issuer (a `VciConfig` field). | Your production issuer base URL. It must expose valid issuer metadata and authorization metadata. |
| `ClientAuthenticationType.AttestationBased(clientId = …)` | Wallet authenticates with wallet/key attestation, identifying itself with `clientId`. | Use if your issuer requires wallet instance or key attestation. Align with Wallet Provider. |
| `clientId` | OAuth client identifier the wallet presents to the issuer under attestation-based client authentication. | Your production wallet client ID, registered with the issuer and aligned with the Wallet Provider. Do not ship a development or sample value. |
| `withAuthFlowRedirectionURI` | Redirect URI for authorization-code issuance flow. | Must exactly match the app manifest and the issuer client registration. |
| `withParUsage` | Pushed Authorization Request use. | Prefer `REQUIRED` if issuer mandates PAR. `IF_SUPPORTED` is acceptable for interoperability where issuer policy allows it. |
| `DPopConfig.Default` | Enables default DPoP behavior for proof-of-possession. | Keep enabled unless your issuer profile explicitly does not support it. |
| `order` | Display order in the "Add document" list. | Use stable order. Put PID issuer first if PID activation is required. |
| `logoOrigins` | Origins, besides the issuer's own, from which the wallet loads the issuer's logo. | Leave empty when the issuer serves its logo from its own origin; otherwise list the `https` origin it uses, such as its CDN. |

The wallet uses an issuer only through its own `VciConfig`; there is no fallback to another issuer's
settings, which would send that issuer's client identifier and attestation to a different host:

* A credential offer is accepted only from a configured issuer (`offerIssuerOf`). The link is read
  with the OpenID4VCI library's own parser, which Wallet Core uses next, and a link that gives the
  offer more than once, in any spelling, is refused. An offer passed by value must name a configured
  `issuerUrl` as its `credential_issuer`; this is decided before any request is made. An offer passed
  by reference must be hosted, over `https`, on the origin of a configured issuer. Wallet Core then
  fetches it, following redirects, and reads the metadata of the issuer it names before the wallet
  can check that this issuer is configured too; an offer from any other issuer is refused at that
  point. A refused offer fails with "This offer comes from an issuer that this wallet is not set up
  to use", and a link that is not a usable offer with "Unable to fetch and process the credential
  offer".
* An offer whose transaction code has no length, a length outside 4 to 6 digits, or text input is
  refused with the transaction-code error (`DocumentOfferInteractor.resolveDocumentOffer`), as on
  iOS.
* Re-issuance, deferred issuance and the authorization redirect use the configured issuer that
  issued the document, and fail if there is none.
* Issuer identifiers are compared exactly. Set `issuerUrl` to the issuer's `credential_issuer` value as
  it appears in its metadata, including or omitting a trailing slash exactly as the issuer does.

Issuer readiness checklist:

* Issuer uses production TLS certificates.
* Issuer metadata is valid and cache behavior is defined.
* Credential configuration IDs are stable.
* Credential display metadata is localized.
* Authorization server redirect URI allowlist includes the production app redirect.
* Issuer supports expected credential formats, for example MSO mdoc and SD-JWT VC.
* Issuer supports the expected proof type and key attestation requirements.
* Issuer revocation/status metadata is available and tested.
* Issuer rate limits and abuse controls account for background reissuance.

## Wallet Provider Host

Current code:

```kotlin
override val walletProviderHost: String
    get() = "https://wallet-provider.eudiw.dev"
```

Production value:

```kotlin
override val walletProviderHost: String
    get() = "https://wallet-provider.example.eu"
```

The app currently calls the following relative paths through `WalletAttestationRepository`:

```text
/wallet-instance-attestation/jwk
/key-attestation/jwk-set
```

Important: these plain-JWK endpoints are suitable only for testing/reference integration unless your
Wallet Provider adds its own strong validation layer. In the reference/demo setup, they accept JWK
material and do not prove that the submitted keys were generated by the Android platform, stored in
hardware-backed keystore, protected by StrongBox, or bound to an uncompromised wallet application.
Do not rely on these endpoints as the production proof that wallet keys are genuine.

For production, prefer Android platform key attestation or an equivalent platform-attested-key
mechanism. The Wallet Provider should validate Android Key Attestation server-side, including the
certificate chain, challenge/nonce, root of trust, revocation status, security level
(`TrustedEnvironment` or `StrongBox` as required), key purpose/algorithm, package identity, signing
certificate digest, and any other policy values required by the Member State or wallet provider.

The Wallet Provider also exposes Android platform key attestation endpoints:

```text
/wallet-instance-attestation/platform-key-attestation/android
/key-attestation/platform-key-attestation/android
```

These endpoints accept Android Key Attestation evidence instead of plain JWKs and are more
appropriate for production deployments. They allow the backend to enforce policy such as
hardware-backed key generation, StrongBox usage where available and required, key origin, key usage,
freshness, and attestation-chain validity. Using them requires the wallet client integration to send
the Android attestation certificate chain and challenge-bound evidence expected by the Wallet
Provider API.

Production implementers must update the wallet client integration as well as the backend. The
current `WalletAttestationRepository` constants point to the plain-JWK paths and send a JWK/JWK-set
payload. A production build that uses Android platform key attestation must change those repository
paths, request/generate Android Key Attestation evidence for the relevant wallet keys, send the
attestation certificate chain and challenge-bound payload expected by the Wallet Provider, and parse
the same `walletInstanceAttestation` / `keyAttestation` response fields after backend
validation. Do not only configure the backend and leave the app calling the JWK endpoints.

Production endpoints must also validate wallet application integrity, not only key provenance. Use
Play Integrity API, a national/app-store equivalent, commercial RASP attestation, or another
server-verified integrity mechanism to bind wallet attestations to the expected package name,
signing certificate, installer/distribution channel, app version, device integrity state, and a
fresh backend nonce.

The production Wallet Provider must:

* Use attestation-backed endpoints for production wallet instance and key attestations.
* Treat plain-JWK endpoints as non-production unless additional server-side validation makes them
  equivalent to the production attestation policy.
* Validate wallet app, device, and key evidence according to your policy.
* Return `walletInstanceAttestation` and `keyAttestation` values in the expected response fields.
* Use HTTPS with production TLS.
* Rate-limit requests.
* Detect abuse and replay.
* Log enough for audit without storing unnecessary personal data.
* Have availability targets aligned with issuance and reissuance flows.

Do not point production builds to EUDI demo wallet-provider services.

## Trust Mark Deployment

Set `WalletCoreConfig.trustMarkSource` to the Trust Mark configuration for your wallet solution.
The default uses a sample Gist resource and a certification-page URL containing
`WALLET_SOLUTION_ID`. Replace the sample values in your production flavor with the correct
resource and certification URLs. See
[Trust Mark configuration](CONFIGURATION.md#trust-mark-configuration) for instructions. The SDK
fetches the resource with a plain GET, and neither the resource nor its image is signed: host it
where your organisation or the Commission controls it, never on an individual's account.

The app does not check certification, recognition, expiry, revocation or wallet instance
attestation, or automatically remove the mark when certification changes. Check that the
displayed image, text and links match your wallet's actual certification status before release.

Test the following:

* On a clean install, the introduction appears after splash, Continue proceeds to PIN setup,
  and later launches skip the completed introduction.
* "About EUDI Wallet" opens from the side menu.
* In both the introduction and About views, "EUDI Wallet Provider Trusted List" opens the
  list of certified wallets and "Certification information page" opens your wallet's
  certification page in the browser. Both links display an external-link icon.
* The Trust Mark image is centered above its localized text and keeps its original proportions.
  Check long text and larger fonts: the text should use the available width, wrap fully and
  scroll with the screen while Welcome's Continue remains available. Resource and image
  loading errors offer Retry.

For an upgrade release, confirm whether existing users should see the introduction and test
that flow.

The app can report when it cannot open a link. Once the browser opens, page-loading and network
errors are handled by the browser.

## Document Issuance Rules

`WalletCoreConfig.documentIssuanceConfig` holds the wallet's per-document issuance and re-issuance
defaults. They apply when the issuer does not advertise a credential reuse policy; when it does, the
issuer's policy takes precedence and these act as the fallback.

```kotlin
DocumentIssuanceConfig(
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
```

The snippet above reflects the `demo` flavor. The `dev` flavor requests a larger batch
(`numberOfCredentials = 60`) for both PID document types. Confirm the batch size you actually ship
per flavor.

Meaning:

| Field                                                        | Meaning                                                                                   | Production decision                                                                   |
|--------------------------------------------------------------|-------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| `CredentialPolicy.RotatingBatch`                             | Batch of credentials rotated across presentations.                                        | Default for most documents where a rotating batch is acceptable.                      |
| `CredentialPolicy.OnceOnly`                                  | Each credential is used once then deleted; issued in batches.                             | Use for unlinkability/privacy-sensitive credentials such as PID where required.       |
| `CredentialPolicy.LimitedTime`                               | A single credential presented repeatedly until it expires.                                | Use where one long-lived credential is acceptable.                                    |
| `numberOfCredentials`                                        | Batch size requested at issuance (`RotatingBatch`/`OnceOnly`; `LimitedTime` is always 1). | Balance privacy, user offline needs, issuer load, and storage size.                   |
| `reissueTriggerUnused` (`OnceOnly`)                          | Reissue when the number of unused credentials is at or below this count.                  | Set high enough to avoid users running out between checks.                            |
| `reissueTriggerLifetimeLeft` (`RotatingBatch`/`LimitedTime`) | Reissue when the remaining validity is at or below this.                                  | Set according to credential validity and issuer SLA.                                  |
| `backgroundInterval` (`ReIssuanceRule`)                      | WorkManager interval for reissuance checks.                                               | Android periodic work minimum is effectively 15 minutes. Avoid excessive issuer load. |

The re-issuance thresholds now travel with each credential's stored policy (`reissueTriggerUnused` /
`reissueTriggerLifetimeLeft`); `ReIssuanceRule` only controls how often the background check runs.

Production questions:

* Which credential types require once-only issuance?
* How many offline presentations must users be able to perform?
* What happens if reissuance fails for days?
* Is authorization fallback allowed for background reissuance?
* Are users informed when documents are close to exhaustion or expiry?
* Does the issuer rate-limit batch issuance?

Recommended:

* Keep PID and privacy-sensitive credentials as once-only if required by the profile.
* Start with a conservative batch size and tune after load testing.
* Monitor reissuance failures.
* Do not silently delete or replace user credentials without clear UX and audit behavior.

What the user sees:

* When an issuer issues only part of a request, the success screen lists the documents that were
  not added, including those refused because their issuer is not trusted. A request that also
  leaves documents deferred ends on the deferred screen, which does not list them. Wallet Core
  reports each failure as an exception, which is not shown to the user (see User-Facing
  Production Behavior); map the causes you want to explain to messages of your own.
* When a background re-issuance fails, the document's details open the issuer card and say that
  the document could not be renewed automatically. The next background check clears the notice
  unless the renewal fails again, and so does a successful **Reissue document** from the card.
* For a revoked, suspended or unrecognised document **Re-issue** is disabled in the toolbar, and the
  issuer card offers no **Reissue document**. A re-issue that reaches
  `DocumentDetailsInteractor.reIssueDocument` for one, for example after a check blocked it while its
  details were open, is refused with "The issuer has revoked or suspended this document, so it cannot
  be reissued." A document whose revocation records cannot be read is refused too, with the read's
  error. A re-issued copy would carry no revocation record, so the block would be lost. Closing the
  refusal reloads the details, so the card shows the block. The background re-issuance skips blocked
  documents too, and `WalletCoreDocumentsController.reIssueDocument` checks again just before it asks
  the issuer, for a document blocked after the background run chose it. If the original is blocked
  while a re-issue is already under way (the issuer's sign-in, or a deferred credential), the
  re-issue still completes, as the issuer chose to issue. While background scheduling is enabled,
  the app requests another revocation check to read the new copy's own status without waiting for
  the periodic interval.

## Document Categories

`WalletCoreConfig.documentCategories` maps credential format identifiers into UI categories.

Examples:

* `eu.europa.ec.eudi.pid.1`
* `urn:eu.europa.ec.eudi:tax:1`
* `org.iso.18013.5.1.mDL`

Production rules:

* Add every production credential type you issue.
* Remove categories that are not supported by your wallet.
* Keep identifiers exactly aligned with issuer metadata.
* Define a fallback category for unknown or future credential types.
* Include localization and icon decisions in UX acceptance testing.

## Revocation And Status Checking

Current default — an interface-level default getter on `WalletCoreConfig`
(`core-logic/src/main/java/eu/europa/ec/corelogic/config/WalletCoreConfig.kt`), not a per-flavor
`override`. A production flavor may override it in its `WalletCoreConfigImpl`:

```kotlin
val revocationInterval: Duration get() = 15.minutes
```

The app enqueues `RevocationWorkManager`, which:

* Loads issued documents.
* Resolves document status through Wallet Core.
* Records a blocked document with the reason: `INVALID`, `SUSPENDED`, or `UNRECOGNISED` for an
  application-specific or reserved status value the wallet cannot interpret. Every record blocks
  presentation.
* Treats `INVALID` as permanent: a later Valid does not clear it, and a later Suspended does not
  downgrade it. A later Valid clears `SUSPENDED` and `UNRECOGNISED`. The document list and the
  document's details show a `SUSPENDED` document as suspended and the others as revoked; all three
  are blocked and counted under the Revoked filter.
* Records a failed refresh (status list unreachable, malformed, or rejected by its signature check)
  for a document that has a status reference, and clears it on the next successful check. Document
  details then open the issuer card and say the status could not be refreshed. The document keeps
  its last known state; a failed refresh does not block it.
* Runs once per process, when the dashboard first opens after login, as well as on its interval.
  Records lost when the database is recreated after a schema change are rebuilt by the first check
  that runs with network access, whether that is the run after login or a periodic one. Until then a
  document revoked before the upgrade is not blocked. The dashboard schedules that run and the
  periodic revocation and re-issuance checks (`DashboardInteractor.scheduleBackgroundWork`, which
  does nothing while the wallet is locked). So the run waits for login, and on a new install nothing
  is scheduled before the first login. By then the host activity has set the session id, so the check
  uses the same Wallet Core instance as the screens; keep that order if you move it. Periodic runs
  are not ordered this way, and neither is a start-up check that an earlier process queued but did
  not finish: WorkManager keeps it, and it runs as the next process starts, like overdue periodic
  work.
* Once scheduled, the periodic checks stay scheduled with WorkManager and run on their interval
  whether or not the wallet is unlocked: while the app is closed, and, as overdue work, when the
  process starts before login, unless stopped for a factory reset. A reset that fails requests
  the periodic checks again, but not the start-up check; after a successful reset they
  start with the dashboard, after the next login. See
  [Data Deletion And Factory Reset](#data-deletion-and-factory-reset) for scheduling and recovery
  details. A periodic run that starts as the process starts, or one already running when the user
  opens the wallet, reads the previous run's session id and opens a second Wallet Core instance on the
  same storage, as in earlier releases. Removing that needs one
  session id shared by the process's workers and screens. To run no check before login, make the
  workers skip while the session is locked; they then run only while the wallet is in use.
* Treats a document whose status reference Wallet Core cannot extract (no `status` claim, or a
  mechanism other than a status list) as having no status, and records no failure for it.
* Status-list trust follows `configureDocumentStatusResolver { configureTrust { … } }`. Under
  `INFORM`, the reference default, a status-list token signed by an untrusted key is still accepted,
  so whoever can serve the list can report a document as Valid. A recorded `INVALID` stays blocked,
  but a suspension can be lifted this way. Use `ENFORCE` in production, for every type you check:
  with the reference classification, which covers only the PID, a global `ENFORCE` fails the status
  check of every other document; see ETSI Trusted Lists.
* Broadcasts changes to refresh UI.

Production decisions:

* Confirm the status mechanism used by each credential type.
* Define expected behavior when status checking fails.
* Define whether invalid/suspended documents are hidden, marked, blocked, or deleted.
* Define battery/network policy.
* Define whether users can manually refresh status.
* Include revocation in incident response and issuer operations.

Recommended:

* Keep background checks, but add observability around repeated failures. The failure record
  (`statusRefreshFailures` table) keeps the time and reason of the last failure.
* Decide whether a document whose status has not been refreshed for a long time should be blocked,
  and how ecosystem-defined application-specific status values should be treated. The reference app
  only reports the first and blocks on the second; both are decided in `revocationUpdateFor` and
  `RevocationWorkManager` (`core-logic`).
* Test revoked, suspended, valid, unreachable, malformed status list, and expired status token cases.

## RQES Configuration

Production RQES config lives in:

```text
business-logic/src/prod/java/eu/europa/ec/businesslogic/config/RQESConfigImpl.kt
```

Current demo/dev code includes:

```kotlin
QtspData(
    name = "Wallet-Centric",
    endpoint = "https://walletcentric.signer.eudiw.dev/csc/v2".toUriOrEmpty(),
    tsaUrl = "https://timestamp.sectigo.com/qualified",
    clientId = "wallet-client",
    clientSecret = "demo-secret-placeholder",
    authFlowRedirectionURI = URI.create(BuildConfig.RQES_DEEPLINK),
    hashAlgorithm = HashAlgorithmOID.SHA_256,
)
```

Do not use demo RQES values in production.

When creating the production flavor, keep the `signingLogger` supplied through `ConfigLogicImpl`
to `RQESConfigImpl` so signing activity appears in transaction history. This remains active when
`printLogs` is false. See the [RQES example](CONFIGURATION.md#general-configuration) and
[transaction-history behavior](#transaction-history).

| Field                     | Meaning                                            | Production value                                                                                                    |
|---------------------------|----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| `name`                    | QTSP display name.                                 | Official QTSP/service name shown to users.                                                                          |
| `endpoint`                | CSC or QTSP signing endpoint.                      | Production HTTPS endpoint from the QTSP.                                                                            |
| `tsaUrl`                  | Timestamp authority URL.                           | Approved TSA endpoint, if required by the signing profile.                                                          |
| `clientId`                | OAuth/client identifier for the wallet or broker.  | Production client ID issued by QTSP or authorization server.                                                        |
| `clientSecret`            | OAuth client secret in current SDK config.         | Avoid embedding real confidential secrets in the app. Use a backend broker or public-client profile where possible. |
| `authFlowRedirectionURI`  | Redirect URI for RQES authorization.               | Must match manifest placeholders and QTSP registration.                                                             |
| `hashAlgorithm`           | Hash algorithm for signing.                        | Use approved algorithm, currently SHA-256 in the reference config.                                                  |
| `documentRetrievalConfig` | Certificate/trust config for retrieving documents. | Replace demo certificates with production trusted certificates.                                                     |
| `signingLogger`           | Records signing activity in transaction history.   | Keep the provided logger configured independently of diagnostic logging.                                            |

Important:

* A value compiled into an Android app is not a secret. Assume attackers can extract it.
* If the QTSP requires a confidential client secret, put the confidential interaction on a backend
  you operate, not directly in the app.
* `printLogs` must be false in production. Current code uses `BuildConfig.DEBUG`, which is good for
  release builds, but verify final artifacts.

## ConfigLogic

`ConfigLogic` controls global behavior:

```kotlin
interface ConfigLogic {
    val appBuildType: AppBuildType
    val appFlavor: AppFlavor
    val appVersion: String
    val rqesConfig: EudiRQESUiConfig
    val changelogUrl: String?
    val forcePidActivation: Boolean
}
```

Production decisions:

| Setting | Production guidance |
| --- | --- |
| `appFlavor` | Must be `PROD` after adding the production enum value. |
| `appVersion` | Must come from controlled versioning, not the placeholder value. |
| `rqesConfig` | Must use production QTSP/TSA config. |
| `changelogUrl` | Use an official release-notes URL, or null if not exposed in app. |
| `forcePidActivation` | Set `true` if the wallet must not operate before PID issuance. |

For Member State wallets, PID activation is commonly a policy decision. If the wallet should only
operate after a PID is issued, set:

```kotlin
override val forcePidActivation: Boolean
    get() = true
```

Then test:

* First launch with no PID.
* Failed PID issuance.
* Revoked PID.
* Expired PID.
* Reissued PID.
* Deleting PID.

## Deep Links And Redirect URIs

Deep-link constants are currently defined in:

`build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt`

Current schemes include:

| Purpose | Current scheme |
| --- | --- |
| Internal wallet navigation | None. Screens are reached by route inside the app: no internal scheme is declared or exported, and no navigation-graph destination registers a deep link |
| OpenID4VP | `openid4vp`, `eudi-openid4vp`, `mdoc-openid4vp`, `haip-vp` |
| Credential offers | `openid-credential-offer`, `haip-vci` |
| OpenID4VCI authorization redirect | `eu.europa.ec.euidi://authorization` |
| RQES OAuth redirect | `rqes://oauth/callback` |
| RQES document retrieval | `eudi-rqes` |

What the app enforces:

* **Verifier redirects.** After a presentation, and after a rejection, the app opens the verifier's
  redirect only when it is an `https` URL with a host, and then as an ordinary web link, normally in
  the browser (`isOpenableExternalLink`, `Context.openInBrowser`). It never passes a redirect to
  its own link handling, so a verifier cannot start an issuance, a presentation or a signing in the
  wallet, and it opens nothing when Android would hand the link back to the wallet. Every other
  scheme is ignored, and the user stays in the wallet.
* **Privacy-policy links** from a verifier's or an issuer's registration certificate follow the same
  rule: one that is not `https` is not shown as a link.
* **RQES callback.** An `rqes` link resumes a signing only when its host and path are those of the
  callback the wallet registered with the QTSP (`BuildConfig.RQES_HOST`, `BuildConfig.RQES_PATH`),
  as on iOS; any other link in the scheme is ignored.
* **RQES document retrieval.** A link with the `eudi-rqes` scheme starts a signing only when its host
  matches `rqesDocRetrievalHost` (`BuildConfig.RQES_DOC_RETRIEVAL_HOST`), compared without regard
  to case. Set an exact host: the manifest's `*.domain` form is not supported by the check, so no
  link would match and the wallet would ignore every such link. The reference value `*`
  (`DeepLinkType.ANY_HOST`) matches any host so that the demo signing flow works; it is for
  development only. The host check only narrows which links
  start the flow, since any web page can use the configured host. The control that decides whether
  a document is retrieved is the certificate trust in `documentRetrievalConfig`
  (`RQESConfigImpl`). A document QR code scanned from **Sign** goes to the RQES SDK without the
  host check.
* **Credential offers** are accepted only from configured issuers; see
  [Issuer Configuration](#issuer-configuration-issuersconfig).

Production concerns:

* Many manifest filters use wildcard hosts (`*`).
* Custom URL schemes can be claimed by other apps.
* Incoming URIs are external input and must be treated as untrusted.
* Redirect URI values must exactly match issuer/QTSP client registration.

Production rules:

* Keep only schemes required by your supported protocols.
* Set `rqesDocRetrievalHost` to the host of your QTSP's document-retrieval links. It drives both the
  manifest filter and the check in `DeepLinkType`.
* Keep verifier redirects out of the wallet's link handling. If you add `https` App Links to the
  wallet, keep relying parties' domains out of them: a redirect that only the wallet can open is
  not opened, and one that the wallet shares with another app shows Android's app chooser. A link
  that arrives and matches no `DeepLinkType` is dropped, so an App Link you add needs a type of its
  own. The main activity is exported, so any installed app can send it any link directly, whatever
  the manifest filters say.
* Expect any link to arrive out of turn: a scheme exported to browsers can be opened by any web page. The
  app ignores an RQES callback or document link that the RQES SDK refuses (for example a callback with no
  signing session in progress) rather than letting the SDK's exception end the process.
* A link the activity was first started with is not opened again when Android recreates the
  activity from saved state. When Android starts the task again without saved state, for example after an app
  update, a force-stop, or once the user has left the wallet with Back and reopens it from Recents,
  it passes that link again, and nothing in the link marks it as old. The activity recognises such a
  restart by `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`, which a relaunch from Recents carries, or by
  `FLAG_ACTIVITY_CLEAR_TASK` when the system UI is the caller Android recorded, as after an update
  on the reference emulator; a referrer the sender put in the intent is ignored. It then skips the
  link if a screen has already used it in this task after login. For that the app keeps one record,
  the task id and a SHA-256 fingerprint of the last launch link used, never the link itself;
  **Reset wallet** clears it. A link the user never got to use, such as one left on the login
  screen, still opens after login, and so does the same link sent again by a browser or another
  app. The check only ever skips a link. Where a device restarts the task in a way it does not
  recognise, the wallet opens the link again: a request already used then fails, and one still
  valid asks for consent and the PIN again. A change to the host activity's launch mode or task
  handling needs this re-tested on the target devices.
* Check the merged manifest for components that libraries export. A component that only the app itself starts can
  be overridden in the app's manifest with `android:exported="false"` and `tools:replace="android:exported"`.
* Do not export internal navigation. Screens are reached by route inside the app. An exported intent filter for an
  internal scheme, or a `navDeepLink` on a navigation-graph destination, lets any app or web page open that screen
  without passing the wallet's login. If screen deep links are needed for UI tests, keep them in a debug-only
  manifest.
* Prefer verified Android App Links (`https`) where protocol and ecosystem allow it.
* If custom schemes are required by the specification, validate every parameter before use.
* Validate issuer/verifier hostnames against allowlists or trust framework rules.
* Reject unknown schemes and hosts.
* Reject oversized URIs.
* Reject malformed JSON in credential offers.
* Reject authorization callbacks with missing or unexpected state where the underlying SDK exposes it.
* Test app-link hijacking and malicious intent injection.

When changing or adding a scheme, update all of these:

* BuildConfig fields in `AndroidLibraryConventionPlugin.kt`.
* Manifest placeholders in `AndroidLibraryConventionPlugin.kt`.
* Intent filters in `assembly-logic/src/main/AndroidManifest.xml`.
* `DeepLinkType` in `ui-logic/src/main/java/eu/europa/ec/uilogic/navigation/helper/DeepLinkAction.kt`.
* Wallet Core `configureOpenId4Vp { withSchemes(...) }`.
* Issuer, verifier, and QTSP client registration.
* Tests for each inbound flow.

## Manifest And Permissions

Production manifest:

`assembly-logic/src/main/AndroidManifest.xml`

Current notable settings:

| Setting | Current value | Production guidance |
| --- | --- | --- |
| `android:allowBackup` | `false` | Keep false. |
| `android:dataExtractionRules` | Present | Explicitly exclude sensitive files, not only empty placeholder rules. |
| `android:fullBackupContent` | Present | Explicitly exclude sensitive files for older Android versions. |
| `android:largeHeap` | `true` | Reassess. Remove unless required and justified by profiling. |
| Main activity | `exported=true` | Required for launcher and external flows. Harden all intent handling. |
| NFC service | `exported=true` with `BIND_NFC_SERVICE` | Confirm required for NFC engagement and test exported-service behavior. |
| FileProvider | `exported=false` | Keep false. Review shared file paths. |
| `HIDE_OVERLAY_WINDOWS` | Requested, not used: no window calls `setHideOverlayWindows(true)` | Use it on the PIN and consent screens if overlay protection is part of your design, or remove the permission. |
| Bluetooth permissions | Requested | Required for proximity. Request at runtime only when needed. |
| Location permissions | Requested | Required for older BLE flows. Explain to users and privacy review. |
| Camera permission | Requested | Required for QR scanning. Request at runtime only when needed. |

Backup rules:

`business-logic/src/main/res/xml/backup_rules.xml`

`business-logic/src/main/res/xml/data_extraction_rules.xml`

The current files are mostly placeholders. Production should explicitly exclude:

* Wallet document storage.
* SQLCipher database.
* DataStore preferences.
* Tink keyset preferences.
* Logs.
* Cache files containing QR, documents, PDFs, or transactions.
* Any exported or temporary signing documents.

Example shape:

```xml
<data-extraction-rules>
    <cloud-backup disableIfNoEncryptionCapabilities="true">
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
    </cloud-backup>
    <device-transfer>
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
    </device-transfer>
</data-extraction-rules>
```

Adapt the exact domains and paths after inspecting final storage locations.

## Network Security

Current network security config:

```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
</network-security-config>
```

Keep cleartext traffic disabled.

Production network rules:

* Never use the trust-all self-signed certificate code from the local build documentation in
  production.
* All issuer, verifier, wallet provider, QTSP, TSA, status, certificate revocation (CRL and OCSP),
  analytics, and telemetry endpoints must use HTTPS. For CRL and OCSP this binds the certificate
  authorities that publish them; see [ETSI Trusted Lists (LoTE)](#etsi-trusted-lists-lote).
* Certificate errors must fail closed.
* Hostname verification must remain enabled.
* Consider certificate pinning for your own high-value backend endpoints.
* Define pin rotation and backup pins before enabling pinning.
* Do not pin third-party public services unless you own the operational risk.

Optional pinning strategy:

* Pin wallet-provider and Member State issuer backends.
* Do not pin all relying-party verifier endpoints unless you control the trust framework and
  rotation process.
* Keep at least one backup pin.
* Monitor pinning failures.
* Provide an emergency app update path.

Ktor client:

`network-logic/src/main/java/eu/europa/ec/networklogic/di/NetworkModule.kt`

Current release behavior:

* `debug`: `LogLevel.BODY`
* `release`: `LogLevel.NONE`

Keep `LogLevel.NONE` for production. Add tests or release checks to ensure release network logging
is disabled.

Timeouts and response size: the same client, which Wallet Core uses for issuance, presentation,
trusted lists, status lists and the Trust Mark (through `withKtorHttpClientFactory`), and which the
app uses for wallet attestation, applies the limits of
`NetworkConfig` in `network-logic`. In the reference configuration a connection must be set up
within 15 seconds, data must keep arriving within 30 seconds, a whole request must end within 60
seconds, and a response body may hold at most 10 MiB. A body whose `Content-Length` passes the limit
is refused before it is read, and any other body as soon as more than the limit has arrived
(`ResponseSizeLimit`). A request past a limit fails as a network error, so an issuance or
presentation link cannot make the wallet wait on, or fill its memory from, a server the link names.
The RQES SDK's requests, including the document retrieval an `eudi-rqes` link starts, use the SDK's
own clients and are not covered. On the reference services the largest responses are issuer
metadata (about 100 KB) and trusted lists (30 to 55 KB); set the limits for your own services and
the networks your users have. `ResponseSizeLimit` reads an internal Ktor API; run its tests again
after every Ktor upgrade.

Images from metadata:

* An issuer's logo is loaded only from an `https` URL on the issuer's own origin, or on an origin
  listed in that issuer's `VciConfig.logoOrigins` (`issuerLogoOrNull` in `core-logic`); otherwise
  the app shows its placeholder. The reference configuration lists `https://eudiw.dev` for the
  `issuer-backend` issuers, which serve their logo there.
* Every remote image, including the Trust Mark image, is loaded only from an `https` URL; local
  schemes such as `content://`, `file://` and `android.resource://` are refused
  (`remoteImageSourceOrNull` in `ui-logic`). A redirect is followed only within the origin of the
  image asked for and over `https`. A download stops after `MAX_REMOTE_IMAGE_BYTES` (2 MiB), and
  Coil decodes to the size shown, at most 4096 pixels a side. An SVG within that size is still
  parsed and drawn, so a hostile but allowed source can slow the screen down.
* Images embedded in documents, such as the portrait or the signature, are read from their header
  first. One that declares more than `MAX_EMBEDDED_IMAGE_PIXELS` (40 megapixels) is not shown, and a
  larger one is downsampled by a power of two to at most `MAX_EMBEDDED_IMAGE_DIMENSION` (2048 pixels)
  on its longest side.

## Local Development With Self-Signed Certificates

The build and configuration docs now recommend debug-only CA trust for local services.

Production rule:

Do not add or ship any code that trusts all certificates or disables hostname verification.

For production-like test environments:

* Use an internal CA installed as a debug-only trust anchor.
* Put debug-only network security config under a debug source set.
* Keep production source sets strict.
* Add CI checks that search for `TrustAll`, `HostnameVerifier { _, _ -> true }`, and
  `checkServerTrusted` no-op implementations.

## Storage And Local Data Protection

The project currently uses several local storage layers.

### Encrypted Preferences

`PrefsControllerImpl` uses:

* Jetpack DataStore.
* Tink `AeadSerializer`.
* AES-256-GCM key template.
* Android Keystore backed master key URI.

When Tink cannot create the Keystore master key, or cannot use it right after creating it,
`AndroidKeysetManager` does not fail: it writes the keyset in cleartext to the app's shared
preferences and returns a working AEAD. (It does fail when an existing master key is unusable or an
encrypted keyset cannot be decrypted.) A keyset written in cleartext stays so after the Keystore
works again, and `isUsingKeystore` then reports true. The preferences hold the PIN hash and the
database key, so they would be encrypted with a key stored beside them. `PrefsControllerImpl`
therefore checks `isUsingKeystore` and the stored keyset before it opens the preferences. A keyset
that is not protected and has encrypted nothing yet is discarded and created once more; if it is
still not protected, or the preferences already exist, `KeystoreUnavailableException` is thrown and
the wallet does not start.

The master key does not require user authentication. The background workers that refresh document
status and re-issue documents open the database while the wallet is locked, and the database key
is in these preferences, so a master key bound to user authentication would stop them. The
preferences are protected by the Keystore binding and the app sandbox; the PIN and biometric
prompts protect the use of the wallet.

Production guidance:

* Keep encrypted DataStore for low-volume sensitive preferences.
* Exclude DataStore files and Tink keysets from backup and device transfer.
* Replace the failed start with your own handling when the Android Keystore key is unavailable or
  invalidated, for example an error screen with a support route. Do not fall back to a keyset
  outside the Keystore.
* If your policy requires the preferences key to need user authentication, move the database key
  and the background work out of these preferences first.
* Avoid storing long-lived tokens unless strictly required.
* Do not store PID attributes in preferences.

### PIN Storage

`PrefsPinStorageProvider` stores:

* Random 32-byte salt.
* PBKDF2-HMAC-SHA256 hash.
* 210,000 iterations.
* Constant-time hash comparison.
* Encrypted preference storage through `PrefsController`.
* An empty PIN is never stored: `setPin` refuses it, as on iOS.

Production guidance:

* Consider binding PIN verification to a hardware-backed key operation.
* Benchmark PBKDF2 iterations on the slowest supported device.
* Consider memory-hard KDF options if supported by your platform/security policy.
* Add telemetry for repeated failed attempts without logging PIN values
  (the reference implementation does not emit lockout metrics).

### PIN Throttle And Lockout

The reference implementation includes an escalating client-side lockout
enforced by `PinThrottleController` and persisted via the encrypted
`PrefsController`. Policy is configured through `AuthenticationConfig`
(see [CONFIGURATION.md#pin-throttle-configuration](CONFIGURATION.md#pin-throttle-configuration)).

Persisted state keys (encrypted DataStore):

* `PinFailedAttempts` — current consecutive failure counter.
* `PinLockoutLevel` — monotonic counter indexing
  `AuthenticationConfig.pinLockoutDurations`.
* `PinLockoutStartedAtElapsed` — the elapsed real time (`SystemClock.elapsedRealtime()`, which
  counts sleep and ignores the date and time settings) when the current lockout began.
* `PinLockoutDuration` — the length of the current lockout, in milliseconds.
* `PinLockoutBootCount` — the boot the lockout began in (`Settings.Global.BOOT_COUNT`).
* `PinLockoutStartedAt`, `PinLockoutEndsAt` — wall-clock values of earlier builds, removed with a
  successful authentication.

Default policy (`AuthenticationConfigImpl`):

| Event                                       | Behavior                                     |
|---------------------------------------------|----------------------------------------------|
| 3 consecutive failed attempts               | First lockout: 30 seconds.                   |
| 3 more failures after lockout ends          | Second lockout: 90 seconds.                  |
| 3 more failures after that                  | Third lockout: 5 minutes.                    |
| Each subsequent batch of 3 failures         | 5 minutes (last list entry reused).          |
| Successful PIN or biometric authentication  | Counters and lockout level reset.            |
| App kill                                    | Lockout window persists.                     |
| Device reboot                               | The lockout begins again in full: the elapsed real time restarts at boot. |
| Device date or time changed                 | No effect: the lockout runs on the elapsed real time, so moving the date forward does not end it early. |
| Preferences cannot be read                  | Counts as a lockout of the longest configured duration, never as extra attempts. |
| Lockout over, then device reboot            | No new lockout once the wallet has seen it end (the login or change-PIN screen counting down to it or opened after it, or a PIN entered after it), which clears it and keeps its level; a lockout that ended while the app was closed begins again in full if the device reboots first. |
| Update from a build with the wall-clock lockout | Ends a lockout in progress: those values are not read; the level is kept. |
| App reinstall                               | State is wiped together with app data. Require re-enrollment unless secure backup/recovery is explicitly implemented. |

Lockout counters are persisted in the encrypted DataStore, not just in
UI state. The PIN input is disabled in both the login screen
(`BiometricViewModel`, via `BiometricInteractor`) and the change-PIN
validation step (`PinViewModel`, via `QuickPinInteractor`) while locked. The biometric icon on the login screen
stays enabled during PIN lockout and acts as an escape hatch — a
successful biometric authentication clears the throttle.

Production gaps to address:

* Decide what happens once the final lockout tier is reached. The
  reference implementation cycles at the last list entry indefinitely;
  production policy may instead require device-credential step-up,
  wallet wipe, or a support recovery flow.
* Decide whether to step up to device credential at high failure
  counts (e.g. after 10 cumulative failures).
* Add server-side risk signals; the current lockout is purely
  client-side and is wiped by reinstall.
* Add telemetry for lockout events without logging PIN values.
* Combine with RASP and server-side risk checks where applicable.

### SQLCipher Room Database

`LogicStorageModule` uses SQLCipher through:

```kotlin
.openHelperFactory(SupportOpenHelperFactory(key))
```

The database stores:

* Bookmarks.
* Revoked document IDs and the status that blocked them.
* Transaction logs.
* Failed reissuance document IDs.
* The issuer-trust verdict for each document.
* The last failed status refresh for each document.

**Transaction log contents.** Wallet Core produces each history entry in the TS10 model, and the
app stores it as JSON in the `transactionLogs` table. The log records the relying party, the
attestation types and the identifiers of the attributes requested and presented, never their
values; this is the rule the ARF and the Implementing Acts set for it. Since release 2026.10.43
(Wallet Core 0.31.0):

* a presentation entry records the requested and presented claims as attestation type (mdoc
  `docType` or SD-JWT VC `vct`) and claim paths, without values;
* when registration checking is on and the request's registration certificate authenticates
  (valid signature, trusted signer), the entry records the relying party's registered name,
  identifier, contacts, intermediary, purpose, privacy policy, registrar and supervisory
  authority, even if the certificate then fails its binding, validity or revocation checks.
  Otherwise only the relying party's name is recorded;
* the entry also records the `transaction_data` the request carried, decoded from base64url and
  otherwise unchanged. For a signing request this includes document names, document locations,
  hashes, any one-time password for document access and the whole content of a document carried
  in the request;
* issuance entries record the attestation types, the number of credentials requested and issued,
  and the issuer. Signing entries record the QTSP, the signing certificate's serial number, the
  document's name and size, and the DTBSR.

Beside each presentation entry, the app records whether the relying party's registration was
verified (`registrationCheck` and `registrationFailureReason`; see Transaction history). Wallet
Core does not keep it in the entry.

Entries have no time or count limit, and removing a document leaves them in place (see
[Data Deletion And Factory Reset](#data-deletion-and-factory-reset)). Entries are removed only
when the user deletes one, by **Reset wallet**, when the history is read and an entry cannot be
decoded, for example after a Wallet Core update changed its format, when the database key is
missing at start-up and the database file is deleted, and when an app update raises the database
version. In the last case the destructive fallback below recreates every table, so the transaction
history, bookmarks, data-deletion and DPA-report attempts, failed re-issuance records, issuer-trust
records, revocation records and failed status refreshes are lost. Every document then shows its
issuer as not verified until it is issued again, and the next status check rebuilds the revocation
records. State this in the release notes of an update that changes the schema.

Current risk:

```kotlin
.fallbackToDestructiveMigration(true)
```

Production guidance:

* Replace destructive migration with explicit Room migrations. The transaction history is a legal
  record and must survive an update.
* Add migration tests.
* The transaction history is a legal record, meant to stay until the wallet is reset or
  uninstalled. The reference app therefore sets no retention limit and keeps it when a document is
  removed, but still loses it in the cases listed above, including an update that changes the
  schema. The user can delete single entries, and **Reset wallet** deletes them all. Do not add a retention purge,
  deletion with a document or a bulk "clear history" action. An entry that can no longer be decoded
  is deleted when the history is read, so check that a Wallet Core update still reads the stored
  entries. History is read without paging.
* Minimize transaction log content. Decide whether the raw `transaction_data`, and one-time
  passwords in particular, belong in the log.
* The log is personal data: it shows which relying parties the user dealt with, when, and which
  attributes were disclosed. Cover it in the DPIA.
* Wipe database key material from memory where practical after opening the DB.
* Exclude DB files from backup and device transfer.

Example:

```kotlin
Room.databaseBuilder(
    context,
    DatabaseService::class.java,
    DatabaseService.DATABASE_NAME
)
    .openHelperFactory(SupportOpenHelperFactory(key))
    .addMigrations(MIGRATION_2_3)
    .build()
```

### Data Deletion And Factory Reset

Removing a document deletes, besides the credential and its keys in Wallet Core:

* its revocation record, issuer-trust verdict and last failed status refresh;
* its bookmark and its failed re-issuance record.

These deletions are best effort: a storage error leaves records behind rather than reporting a
completed removal as failed, and the factory reset removes what is left.

The transaction history stays, and Wallet Core adds its own record of the deletion. The user can
delete single entries, and **Reset wallet** deletes them all.

A re-issuance replaces the original document. Wallet Core deletes the original once a new
credential has been issued, and for a deferred re-issuance only when the deferred credential
arrives. The app copies the original's bookmark to the new documents and deletes the original's
other records once Wallet Core has deleted it; after a deferred credential arrives, it deletes the
records of every document Wallet Core no longer holds.

**Settings → Reset wallet** is the factory reset. It asks for confirmation, lists what will be
deleted, and requires the PIN or a biometric. `WalletCoreResetController` then, while the app keeps
running:

1. blocks new checks before cancelling the scheduled revocation and re-issuance work
   (`WalletWorkScheduler.stop`), then waits up to `WalletCoreConfig.workerStopTimeout` (30 seconds
   by default) for running work to finish. Wallet Core finishes a re-issuance it has started even
   when its worker is cancelled, so the worker follows it to its outcome first. If a worker is still
   working when the wait ends, the reset fails before anything is deleted;
2. deletes every document Wallet Core stores, the main PID last, so that their keys are removed as
   Wallet Core intends. Meanwhile it holds the Digital Credential API registration, and then clears
   the registry, which the system keeps outside the app's data
   (`WalletCoreDCAPIRegistrationController`);
3. empties every table of the app's database, and its write-ahead log;
4. clears the preferences in a single edit, which hold the PIN, the biometric choice and the other
   settings, keeping only the current session id and the database key, which the running app needs;
5. deletes the log files and the app's cached files, which include documents signed with RQES and
   the logos of issuers and relying parties;
6. locks the session.

While scheduling is stopped, start-up checks and additional revocation requests, including a request
from a finishing re-issuance, are discarded. They are not saved for later. The stop guard and enqueue
calls share a lock, and dashboard resumes cannot reopen scheduling while the reset is running.

If a step fails, the reset clears `WalletResetStatus.isResetting` before calling
`WalletWorkScheduler.schedule`. That call reopens scheduling and submits the periodic revocation
and re-issuance checks to WorkManager. The revocation worker reads the issued documents still in
the wallet when it runs. After a successful reset scheduling stays stopped until the dashboard opens
after the next login, as after a first install.

After a successful reset, the wallet starts again at the splash screen, which leads to onboarding,
as after a first install. The app does not close. Once started, the reset runs to the end even if its screen is
closed, and its screen is a protected-flow screen (`protectedFlowScreens`), so the background lock
waits `protectedFlowLockTimeout` while it runs. A link or Digital Credential API request that
arrives while it runs is dropped, and a link the app kept for after login is forgotten. If a
document cannot be deleted or a step fails, the user can retry, or close the screen, which also
starts the wallet again at the splash screen. A reset screen restored after the process ended finds
the wallet locked and does not start; the lock routes to the login screen. When the database key
is missing at start-up, any existing database file is deleted, since it could not be opened, and a
new database is created.

The reset keeps the following, none of which holds the user's personal data:

* the keys Wallet Core keeps per issuer for wallet attestation and DPoP (`no_backup/wallet-attest.bin`
  and `no_backup/dpop_metadata`), which Wallet Core offers no way to delete. An issuer can therefore
  recognise the installation after a reset;
* Wallet Core's storage files in `no_backup` (the document store and `issuance_metadata.db`). Wallet
  Core deletes the documents and their issuance details from them, but SQLite can keep the freed
  pages in the file until they are reused;
* the app's database file and its key, with every table and the write-ahead log empty, and the key
  that encrypts the preferences;
* Android Keystore keys the app created, such as the key behind biometric login, which are no longer
  used.

Clearing the app's data from the system settings, or reinstalling, removes these as well.

Production rules:

* Any local store you add must be covered by both paths. Records scoped to a document must be
  deleted in `deleteDocumentRecords` in `WalletCoreDocumentsControllerImpl`.
* Data kept outside the app's data directory, for example in shared storage or another app, is not
  cleared by the reset and must be given its own deletion step, as the Digital Credential API
  registry is.
* A PIN, biometry or throttle storage provider that keeps its data outside `PrefsController` (see
  [CONFIGURATION.md](CONFIGURATION.md#pin-storage-configuration)) must be cleared by the reset too.
* Keep the confirmation and the authentication check in front of the reset.
* If an issuer must not recognise the installation after a reset, tell users to clear the app's
  data from the system settings instead, or delete the Wallet Core key stores as part of the reset.
* The reset does not reach data held by the Wallet Provider, issuers or verifiers. State in the
  privacy notice how users can ask for that data to be deleted.

## Logging And Audit

The app has:

* Logging disabled in release on the app's own HTTP client (`NetworkModule`). Wallet Core's own HTTP
  clients are not covered; see [Diagnostic logging](#diagnostic-logging).
* Wallet Core logging routed through `WalletCoreLogController`, which drops records above
  `EudiWalletConfig.logLevel`.
* File logging through `LogControllerImpl`, in debug builds only (`LogController.shouldWriteLogFile`,
  set from the build type in `provideLogController`). A release build writes no log file, deletes
  files an earlier build left, and Settings offers no "Retrieve logs".
* Transaction logging through `WalletCoreTransactionLogControllerImpl`.

### Transaction history

Wallet operations and RQES signing activity appear in the app's locally stored transaction history.
Keep transaction logging configured for every flavor, including release builds. Later updates to a
transaction refresh its existing record.

When a presentation includes recorded signing-request information, its details show one
**SIGNING REQUEST / Signature details** section. It is read-only, including any document locations.
Signing results appear in separate history entries.

Available hashes and expected document checksums are displayed as supplied information. They do
not represent a document-integrity verification result. One-time passwords for document access
are not shown again in history, although the log keeps them. What each entry stores is described
under [SQLCipher Room Database](#sqlcipher-room-database).

Wallet Core records a relying party's registration details in the entry even when the
registration certificate failed its binding, validity or revocation checks, and does not record
the outcome. The app records it beside the entry (`registrationCheck` in `transactionLogs`) when
the request arrives, matching the
request to the pending entry Wallet Core has just written: in Wallet Core 0.31.1, as in 0.31.0, its
transaction listener is registered first and writes that entry before the app's listener sees the request.
The app checks that the relying party in the entry is the one in the request before recording
anything. If a second entry is written before the first request reaches the app's listener, as
can happen when two requests are dispatched from different threads, neither outcome is recorded.
An entry for which no outcome was recorded counts as not verified.

`TestWalletCoreTransactionLogController` runs Wallet Core's own `PresentationLogger` to check
that it writes the entry while it handles the request. It does not check the registration order
in `LogicCoreModule.provideEudiWallet` or how Wallet Core dispatches events to its listeners;
re-check both after every Wallet Core upgrade. Do not call Wallet Core's
`PresentationLogger.removeAllTransferEventListeners()`: it adds back only its own listener, and
every later entry would then read as not verified.

When an entry carries details from a registration certificate (contacts, an identifier, an
intermediary or registration fields) and the registration was not verified, its details say so
and the deletion-request and DPA-report screens repeat the warning. When the certificate was
issued to a party other than the one that asked (`NOT_BOUND_TO_REQUESTER`), its contacts are not
that party's, and no deletion
request is offered. A DPA report is still offered in every case.

Data-deletion request and DPA-report attempts are associated with their originating presentation
and communication method. They appear in that presentation's history rather than the main
transaction list. An attempt records that the external application opened; it does not confirm that
a request was sent, received, or fulfilled. Deleting a presentation also deletes its linked attempts.

Deleting a pending issuance log removes its current entry. When issuance later completes, a log
can appear again. Log deletion does not cancel issuance.

Removing a document keeps its history; see
[Data Deletion And Factory Reset](#data-deletion-and-factory-reset).

### Diagnostic logging

Production logging rules:

* Do not log PID attributes.
* Do not log credential payloads.
* Do not log SD-JWTs, mdocs, status tokens, DPoP proofs, authorization codes, access tokens, or refresh tokens.
* Do not log private keys, public key attestation payloads beyond necessary metadata, or PIN/auth material.
* Do not log full OpenID4VP or OpenID4VCI request objects unless redacted.
* Do not expose logs to other apps except through a user-approved support flow.
* Encrypt or avoid persistent log files.
* Define retention and deletion.
* Provide a redaction layer for all logs.

Wallet Core logs its own HTTP traffic. When `EudiWallet` is built, it installs Ktor's `Logging`
plugin at `LogLevel.ALL` on the clients it uses for OpenID4VCI issuance, remote OpenID4VP
presentation and the Trust Mark. It does so whether or not `withLogger(...)` is called, since it
falls back to its own logger; that logger drops the records at the default `logLevel`
(`LEVEL_INFO`), but a supplied logger, such as the app's, receives them all. It also overrides the level of a client supplied with
`withKtorHttpClientFactory`, so the app's `LogLevel.NONE` for release does not apply to these
requests. Each request and response, headers and body, reaches the logger as a debug record. This
includes authorization codes, access and refresh tokens, DPoP proofs, client attestations, request
objects with their transaction data, and the `vp_token` when the verifier does not ask for an
encrypted response. Responses from the credential endpoint are encrypted by default and are
logged as received; deferred credential responses are requested without encryption and are
logged in clear.

Wallet Core applies `EudiWalletConfig.logLevel` only in its own logger, which it uses when no logger
is supplied, so the logger the app supplies receives every record. The app therefore applies
`logLevel` itself: `WalletCoreLogController` drops the records above it before they reach
`LogController`, which writes to logcat and, in debug builds, to the log files that **Settings →
Retrieve logs** shares. `WalletCoreConfigImpl` sets `LEVEL_DEBUG` for debug builds and `LEVEL_INFO` for release
builds, so a release build keeps none of Wallet Core's debug records. Those include the HTTP
traffic above and other protocol data: the remote OpenID4VP `vp_token` before it is sent, whether
or not the response is encrypted, the Digital Credential API request and `vp_token`, and the mdoc
`DeviceResponse` before it is encrypted. A debug build logs all of it; never ship one, and keep
`LEVEL_INFO` or lower for release. Logging from the app's own code is not filtered this way; see
below.

Current `LogControllerImpl` always plants a `Timber.DebugTree()`, release builds included; it plants
the file logger only when `shouldWriteLogFile` is true (debug builds). For production,
consider:

* Planting a production-safe tree only for release.
* Raising minimum file log priority.
* Removing file logging entirely unless support policy requires it.
* Adding a redacting logger facade.
* Making log export explicit and user-controlled.

Example production-safe pattern:

```kotlin
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree(), fileLoggerTree)
} else {
    Timber.plant(ReleaseRedactingTree())
}
```

## Screenshots, Overlays, And Sensitive UI

Production wallets should protect sensitive screens:

* PIN entry.
* Biometric prompt context screens.
* Credential detail screens.
* Presentation request screens.
* QR code engagement screens.
* RQES document/signing screens.

Recommended controls:

* Use `WindowManager.LayoutParams.FLAG_SECURE` on sensitive screens.
* On Android 13+, use `setRecentsScreenshotEnabled(false)` where appropriate.
* Use Compose `SecureFlagPolicy.SecureOn` for dialogs/popups where available.
* Hide sensitive values when the app is backgrounded.
* Consider overlay detection or blocking for critical consent and PIN screens.
* Keep `HIDE_OVERLAY_WINDOWS` only if tested and justified.
* Do not show PID attributes in notifications.

The reference app offers the first control as a setting. With
`ConfigUILogic.shouldPreventScreenCapture` set to true, the host activity's window carries
`FLAG_SECURE`: screenshots and screen recordings show no wallet content, the recent-apps thumbnail
is blank, and the Compose dialogs and bottom sheets shown over the window inherit the flag. It is
false in the reference configuration, so that the screens can be captured in development and
testing: set it to true for production. `ConfigUILogicImpl` is in `ui-logic`'s main source set and
applies to every flavor; to keep capture in development builds only, derive the value from
`ConfigLogic.appFlavor` or `appBuildType`. It does not cover the RQES SDK's screens, which run in an
activity of their own. With the setting on, `setRecentsScreenshotEnabled(false)` and
`SecureFlagPolicy.SecureOn` add nothing for the wallet's window. The remaining controls are left to
implementers: the manifest requests `HIDE_OVERLAY_WINDOWS`, but no window calls
`setHideOverlayWindows(true)`, so the permission has no effect. Remove the permission if you do
not use it.

Test cases:

* Screenshot on each sensitive screen.
* Screen recording.
* Recent apps thumbnail.
* Overlay window attempt.
* Accessibility service inspection, according to policy.
* Background/foreground transitions.

## Authentication And Biometrics

The app supports PIN and biometrics. The biometric crypto path uses Android Keystore with:

* AES/GCM/NoPadding.
* `setUserAuthenticationRequired(true)` when requested.
* `AUTH_DEVICE_CREDENTIAL` or `AUTH_BIOMETRIC_STRONG` on Android 11+.
* `setInvalidatedByBiometricEnrollment(true)`.

Production decisions:

| Decision | Guidance |
| --- | --- |
| Biometric strength | Prefer `BIOMETRIC_STRONG` for sensitive operations. |
| Device credential fallback | Decide whether fallback is allowed for each operation. For highest assurance, do not silently weaken biometric-only requirements. |
| Key invalidation | Keep invalidation on new biometric enrollment. |
| Biometric unlock switch | Turning biometric unlock on in Settings first asks for the wallet PIN (`CommonScreens.Biometric`, as the factory reset does), then for a biometric; turning it off accepts the PIN or a biometric. Someone holding the unlocked phone who has enrolled a new biometric therefore cannot turn biometric unlock on with it. Keep the PIN step if you change the Settings screen. |
| Session duration | The wallet locks after `AuthenticationConfig.backgroundLockTimeout` in the background (default 60 seconds) and after `idleLockTimeout` without interaction on any of the wallet's screens (default 5 minutes). The RQES SDK's signing screens are a separate activity whose interaction the wallet cannot see: while they are in front the idle lock gives way to `protectedFlowLockTimeout`, and returning to the wallet after that long requires login. While an issuance, presentation, signing or factory reset is in progress, the background limit is `protectedFlowLockTimeout` instead (default 10 minutes), because issuance and signing send the user to a website and back, and a reset must not be interrupted. Tune all three for your assurance level; production wallets usually lock sooner. Every credential release already asks for the PIN or a biometric. |
| PIN length | Current UI defaults to 6 digits. Confirm policy. |
| PIN strength | A new PIN is refused when it is one digit repeated (`000000`), an ascending or descending run (`123456`, `654321`), or on `AuthenticationConfig.commonPins`, a short list of common PINs. The check applies when the PIN is created and when it is changed. Extend the list to match your policy. |
| Device screen lock | During setup the PIN screen, and at each login the login screen, check `KeyguardManager.isDeviceSecure()`. On a device without a screen lock they show a warning and the user goes on; with `AuthenticationConfig.requireDeviceSecure = true` setup or login stops until a lock is set, and the check runs again when the user returns from the device settings. A screen lock removed after setup is therefore noticed at the next login. Other uses of these screens (changing the PIN, and the confirmation before a presentation, a reset or the biometric switch) are not affected. Wallet Core does not refuse `userAuthenticationRequired = true` on a device without a screen lock: when `EudiWallet` is built it sets the option to false and creates keys without user authentication (`EudiWallet.Builder.ensureUserAuthIsSupported`), and StrongBox falls back the same way. Where key-level protection is required, also set `requireDeviceSecure = true`. With it, removing the screen lock later stops the next login until a lock is set again; decide what happens to keys bound to user authentication, which Android invalidates when the screen lock is removed. |
| PIN lockout policy | Defaults to 3 attempts then escalating lockouts of 30s, 90s, 5m (then 5m repeats). Tune via `AuthenticationConfig.maxFailedPinAttempts` and `pinLockoutDurations`. Decide whether the final tier should escalate further (wipe, support flow, device credential step-up). |
| Recovery | Define what happens if user forgets PIN or changes biometrics. |

Session lock:

* `SessionController` (`authentication-logic`) records whether the wallet is unlocked. Login and PIN creation
  unlock it (so does changing the PIN, after the current PIN has been checked); the timeouts above lock it,
  and the app then returns to the login screen.
* The app counts as in the background only when its whole process is (`ProcessLifecycleOwner`, observed
  once from `Application`). Another activity of the app, such as the RQES screens, does not count.
* Whether the screen was a protected-flow screen is recorded when the app leaves, so nothing opened or
  restored on return can extend the limit.
* The background time counts from when the app left, also when a login completes after that, for example a
  PIN check that finishes as the user switches away: the app locks on return once `backgroundLockTimeout`
  has passed. The PIN or biometric screen is on `protectedFlowScreens` as a flow's confirmation, but the
  login itself does not count: it counts only when the back stack also holds a screen past login
  (`RouterHost.isInProtectedFlow()`). A background from an earlier absence was already cleared when the app
  came back while locked (`SessionController.onForeground()`). As on iOS.
* Touches and screen-reader actions count as interaction for the idle lock, both in the activity's
  window and in bottom sheets built with `WrapModalBottomSheet`, which draw in a window of their own
  (`reportAccessibilityInteraction`, `reportingUserInteractionTo`). A dialog or popup you add in its
  own window must report interaction the same way.
* A link that arrives while the wallet is locked is kept and opened after login. Screens restored before
  login, such as the dashboard after the activity is recreated, do not receive it. The issuance redirect
  is the exception: it belongs to the issuance that was in progress, so a lock during that issuance means
  starting it again.
* A new process always starts locked. Whenever the wallet is locked, which includes when Android restores
  the app's screens after ending its process, the app returns to login if any screen on the back stack is
  not a login screen. The whole back stack is checked because the login screens are also used after
  login, for example the PIN screen that confirms a presentation.
* `ConfigUILogic` lists the screens involved: `loginScreens`, which a locked wallet may show, and
  `protectedFlowScreens`, on which the background limit is `protectedFlowLockTimeout`. Add any screen you
  introduce to the right list; a protected-flow screen left off the list locks after the normal
  background timeout, and a login screen left off it sends the user back to login.
* The proximity QR screen is not on `protectedFlowScreens`. When the activity stops while it waits for
  a reader (the user leaves the app or turns the screen off), it stops the session (BLE advertising
  and NFC engagement) and closes, unless another screen, such as the login screen after a lock, has
  taken its place (`ProximityQRViewModel`, `Event.OnStop`). A configuration change, a dialog over the
  screen, or a session already handed to the request screen keeps it running.
* A flow that leaves the app from a screen that is not on the protected list, such as a re-issuance from
  document details, is marked with `SessionController.beginExternalFlow()` and `endExternalFlow()` so
  that the same limit applies. Mark any such flow you add. Another activity of the app, such as the RQES
  SDK's, needs no marking: `registerSessionLockObservers` reports it, and the same limit applies
  from the moment it covers the wallet's screen until the wallet's screen is back. The idle timeout
  does not run meanwhile, since the wallet cannot see interaction there. The window follows the
  wallet's screen, not the signing session. If the SDK ends by opening a browser, the longer limit
  holds until the user next returns to the wallet. Once the wallet has shown a presentation the SDK
  asked for, a later visit to the signing service from a screen off the protected list has the
  normal background limit, so a login can come before the signing resumes.
* The prompt before each credential release is a screen in the app. With
  `userAuthenticationRequired = false` the keys themselves do not require user authentication, so a route to
  the send step that skipped that screen would be unprotected. Keep internal screens unexported (see Deep
  Links And Redirect URIs), and use `userAuthenticationRequired = true` where the assurance level requires
  key-level protection.

Critical point:

Local authentication should protect local key use and user consent, but server-side authorization and
trust decisions must not rely only on UI state.

## Analytics And Telemetry

`analytics-logic` supports optional providers by implementing `AnalyticsConfigImpl`.

Production telemetry rules:

* Do not send PID attributes.
* Do not send credential IDs unless pseudonymized and approved.
* Do not send verifier request contents.
* Do not send issuer tokens.
* Do not send precise timestamps that allow unnecessary profiling unless justified.
* Provide opt-in/opt-out where required.
* Update privacy notice and DPIA.
* Disable third-party analytics in high-assurance flows unless formally approved.

Recommended event examples:

* `issuance_started`, without document attributes.
* `issuance_failed`, with coarse error category.
* `presentation_cancelled`, without verifier payload.
* `revocation_status_refresh_failed`, with coarse cause.

Avoid:

* Full URLs containing query parameters.
* User names, birth dates, addresses, document numbers.
* Raw exception messages from protocol libraries if they may include payloads.

## RASP And App Hardening

Runtime Application Self-Protection is needed because Android apps can be extracted, patched,
repackaged, instrumented, and run on compromised devices.

RASP does not make the app impossible to attack. It increases cost, improves detection, and gives
the backend risk signals.

Use one of the following hardening paths, or a layered combination.

## Hardening Path A: Google Play Integrity API

Use this path if the production wallet is distributed through Google Play and your policy accepts a
Google Play services dependency.

Play Integrity can help identify:

* Unmodified app binary recognized by Google Play (`appIntegrity`).
* Device integrity and signs of compromise (`deviceIntegrity`).
* Whether the app came from Google Play (`accountDetails`).
* Optional environmental risk signals such as app access risk and Play Protect status.

Recommended architecture:

```text
Android app
  -> requests Play Integrity token for a high-risk action
  -> sends token and request nonce to your backend
Backend
  -> verifies/decrypts verdict with Google
  -> validates nonce, package name, certificate digest, version, timestamp
  -> applies risk policy
  -> returns allow, step-up, limited mode, or deny
```

Do not make final trust decisions only on the device. Verify integrity verdicts server-side.

When to request an integrity verdict:

* Wallet activation.
* PID issuance.
* High-risk credential issuance.
* Credential presentation to remote verifiers.
* RQES enrollment or signing.
* Recovery/reset operations.
* Suspicious behavior, for example repeated PIN failures.

Minimum backend checks:

* Nonce matches the action and has not been reused.
* Package name matches production `applicationId`.
* Signing certificate digest matches production release certificate.
* App version is allowed.
* `appIntegrity` indicates the expected app recognition verdict.
* `deviceIntegrity` contains required labels for your policy.
* Verdict timestamp is fresh.
* Request comes from the authenticated user/session where applicable.

Policy examples:

| Verdict outcome | Suggested action |
| --- | --- |
| Genuine app and device | Continue. |
| App not recognized | Block high-risk actions and require reinstall/update. |
| Device integrity missing | Deny high-risk operations or restrict wallet mode, depending on policy. |
| Basic integrity only | Consider read-only or limited mode. |
| Suspicious app access risk | Block sensitive disclosure screens or require step-up. |

Important distribution issue:

Play Integrity can exclude users on devices without Google Play services or devices that do not
meet Google integrity labels. For a public-sector wallet, this is a policy decision. Document it.

## Hardening Path B: DexGuard Or Commercial Protection

Use this path when you need stronger code hardening and runtime protection than standard R8/ProGuard.

DexGuard is an Android application protection product that can provide:

* Advanced obfuscation.
* String/class/resource/native library encryption.
* Control-flow obfuscation.
* Code virtualization.
* Anti-tamper.
* Anti-debugging.
* Root, emulator, hooking, and instrumentation detection.
* RASP checks distributed throughout the app.

Recommended use:

* Protect release builds only.
* Protect protocol, attestation, storage, RASP, and anti-tamper code paths.
* Protect constants that reveal backend behavior, while assuming URLs are still discoverable.
* Integrate RASP responses with backend risk policy.
* Generate mapping files and store them securely for crash analysis.
* Test accessibility, performance, startup, and battery after hardening.
* Test Wallet Core and RQES SDK behavior after obfuscation.

Commercial hardening checklist:

* Enable name obfuscation.
* Enable string encryption for sensitive constants.
* Enable resource encryption where supported.
* Enable tamper/repackage detection.
* Enable debugger detection.
* Enable root and emulator detection.
* Enable hook/framework detection.
* Enable certificate/signature checks.
* Enable response callbacks that can restrict high-risk flows.
* Keep mapping files outside the repo.
* Run full functional and security regression tests on protected artifacts.

Do not rely on a commercial tool alone. Pair it with:

* Server-side attestation.
* Secure storage.
* Strong authentication.
* Secure network policy.
* MASVS testing.

## Hardening Path C: Manual RASP Controls

Use this path when Play Integrity is not acceptable or when commercial tooling is not available.

Manual controls are easier to bypass than mature commercial hardening. Implement them as layered
signals, obfuscate them, and enforce high-risk outcomes on the backend where possible.

### Manual Control 1: App Signature Verification

At runtime, verify that the app is signed by the expected production certificate.

Example shape:

```kotlin
fun isExpectedSigningCertificate(context: Context, expectedSha256: String): Boolean {
    val packageInfo = context.packageManager.getPackageInfo(
        context.packageName,
        PackageManager.GET_SIGNING_CERTIFICATES
    )
    val certs = packageInfo.signingInfo.apkContentsSigners
    return certs.any { signature ->
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(signature.toByteArray())
            .joinToString(":") { "%02X".format(it) }
        digest.equals(expectedSha256, ignoreCase = true)
    }
}
```

Production rules:

* Store the expected certificate digest in an obfuscated form.
* Also validate it server-side through Play Integrity or your own attestation where possible.
* Treat failure as a high-risk event.

### Manual Control 2: Debugger Detection

Example shape:

```kotlin
fun isDebuggerAttached(): Boolean =
    Debug.isDebuggerConnected() || Debug.waitingForDebugger()
```

Use it as a signal. Do not put all protection behind one obvious branch.

### Manual Control 3: Root And System Compromise Signals

Signals can include:

* Known root management packages.
* Writable system paths.
* `su` binary paths.
* Dangerous properties.
* Test keys.
* Magisk or hooking traces.

Example shape:

```kotlin
fun hasSuBinary(): Boolean {
    val paths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/vendor/bin/su"
    )
    return paths.any { File(it).exists() }
}
```

Rules:

* Expect bypasses.
* Avoid collecting excessive device data.
* Combine many weak signals into a risk score.
* Make policy decisions transparent enough for public-sector accountability.

### Manual Control 4: Emulator Detection

Signals can include:

* Emulator build fingerprints.
* Generic hardware names.
* Emulator-specific files.
* Telephony and sensor anomalies.

Use carefully. Some legitimate users may use unusual devices.

### Manual Control 5: Hooking And Instrumentation Detection

Signals can include:

* Frida server ports.
* Loaded libraries with suspicious names.
* Known hooking framework packages.
* Unexpected writable/executable mappings.

Treat as high-risk for signing, issuance, and presentation.

### Manual Control 6: Anti-Tamper Backend Binding

For high-risk operations, send a signed risk envelope to your backend:

```json
{
  "appVersion": "2026.05.1",
  "packageName": "eu.example.wallet",
  "signingCertSha256": "AB:CD:...",
  "deviceRisk": "LOW",
  "debugger": false,
  "rootSignals": [],
  "nonce": "server-issued-nonce",
  "timestamp": "2026-05-08T10:00:00Z"
}
```

The backend should:

* Issue nonces.
* Reject replays.
* Bind the risk envelope to the user session and action.
* Treat device-side claims as hints unless backed by hardware or platform attestation.

### Manual Control 7: Obfuscation With R8

Current release builds set:

```kotlin
isMinifyEnabled = true
isShrinkResources = true
proguardFiles(
    getDefaultProguardFile("proguard-android-optimize.txt"),
    "proguard-rules.pro"
)
```

Production rules:

* Keep minification enabled.
* Remove unnecessary `-keep` rules.
* Keep only what reflection, serialization, Koin, SDKs, and platform callbacks require.
* Verify release artifact behavior after minification.
* Store mapping files securely.
* Consider enabling R8 full mode after compatibility testing. The repo does not set
  `android.enableR8.fullMode`, so it uses the Android Gradle Plugin default. Set it explicitly in
  `gradle.properties` once you have verified compatibility.

## OWASP MASVS Production Alignment

OWASP MASVS v2 is organized by control groups. It no longer uses the old MASVS verification levels
inside the standard itself. Use MASVS together with MASTG, MASWE, and the OWASP MAS Checklist to
define your test profile and evidence.

Official references:

* https://mas.owasp.org/MASVS/
* https://mas.owasp.org/MASTG/

### MASVS-STORAGE

Controls to evidence:

* Sensitive data is stored only in app-private storage.
* Wallet documents and DB data are encrypted at rest.
* DataStore uses authenticated encryption.
* PIN hashes are salted and derived with PBKDF2.
* Backups exclude sensitive data.
* Logs do not contain sensitive data.
* Temporary files are removed.
* Clipboard is not used for secrets.
* Screenshots and recent thumbnails do not expose sensitive screens.

Project-specific actions:

* Review Wallet Core document storage location.
* Exclude SQLCipher DB and DataStore from backup.
* Replace destructive DB migration.
* Redact or remove file logging.
* Add tests that inspect app data after flows.
* Set `ConfigUILogic.shouldPreventScreenCapture` to true.

### MASVS-CRYPTO

Controls to evidence:

* Approved algorithms are used.
* Random values use `SecureRandom`.
* Android Keystore protects keys.
* StrongBox is used where available and required.
* Keys are invalidated or rotated according to policy.
* No hardcoded cryptographic keys are used.

Project-specific actions:

* Review `configureDocumentKeyCreation`.
* Review AES/GCM usage.
* Review Tink DataStore master key.
* Review SQLCipher key generation and storage.
* Confirm credential signing algorithms with the trust framework.

### MASVS-AUTH

Controls to evidence:

* Local authentication is required for sensitive operations.
* PIN storage is resistant to offline attack.
* Failed authentication is rate-limited.
* Biometric authentication is cryptographically bound.
* New biometric enrollment invalidates relevant keys.
* Server-side authorization is enforced for network operations.

Project-specific actions:

* Review the PIN lockout policy (`AuthenticationConfig.maxFailedPinAttempts`, `pinLockoutDurations`).
* Decide device credential fallback policy.
* Consider `userAuthenticationRequired = true` for document keys.
* Test biometric enrollment changes.
* Test authentication bypass attempts.
* Confirm that turning biometric unlock on asks for the PIN.

### MASVS-NETWORK

Controls to evidence:

* Cleartext is disabled.
* TLS validation is strict.
* No trust-all certificate manager is present.
* Sensitive data is not logged in network logs.
* Certificate pinning is used where justified.
* Backend APIs authenticate and authorize requests.

Project-specific actions:

* Keep release Ktor logging at `NONE`.
* Remove self-signed development code from production source sets.
* Pin production wallet-provider or issuer endpoints if policy requires it.
* Verify issuer/verifier/QTSP TLS with automated tests.
* Set the `NetworkConfig` limits for the production services.

### MASVS-PLATFORM

Controls to evidence:

* Exported components are minimized and protected.
* Deep links are validated.
* IPC is explicit and package-scoped where possible.
* FileProvider does not expose broad paths.
* WebViews are hardened.
* Permissions are minimal and runtime-scoped.
* Overlay, screenshot, and notification leaks are addressed.

Project-specific actions:

* Threat-model `MainActivity` inbound intents.
* Tighten scheme and host handling.
* Review NFC service export.
* Review FileProvider paths.
* Request camera/Bluetooth/location permissions only in relevant flows.

### MASVS-CODE

Controls to evidence:

* Dependencies are current.
* SCA is run.
* Unsafe dynamic loading is absent.
* Debug code is absent in release.
* Error handling avoids sensitive disclosure.
* App targets a recent platform version.
* Update enforcement exists.

Project-specific actions:

* Run Dependency Check and generate SBOM.
* Add release checks for demo endpoints.
* Review exception messages shown to users.
* Implement forced update or minimum supported version through backend policy.

### MASVS-RESILIENCE

Controls to evidence:

* App is not debuggable.
* Code is obfuscated.
* App integrity is checked.
* Runtime tampering is detected.
* Root/emulator/hooking/debugging signals are handled.
* Official distribution channel is verified if applicable.

Project-specific actions:

* Use Play Integrity, DexGuard, or manual RASP controls.
* Keep R8 enabled.
* Store mapping files securely.
* Verify release manifest and signing cert.
* Add backend risk enforcement for high-risk actions.

### MASVS-PRIVACY

Controls to evidence:

* Data minimization is applied.
* User consent is clear before disclosure.
* The purpose shown for a presentation request comes from the verifier's registration certificate
  rather than being self-asserted, and a request beyond the registered scope is visible as such.
* Analytics avoids personal data.
* Retention is defined for diagnostic logs and analytics; the transaction history is kept until reset or
  uninstall.
* Logs and transaction history are privacy-reviewed.
* Permissions are justified.
* Privacy notice is accurate.

Project-specific actions:

* Review presentation request UI for clarity.
* Review how the registered identity, purpose, and privacy policy are surfaced on the consent
  screen, and how an unregistered or over-asking verifier is distinguished from a compliant one.
* Review transaction log contents.
* Review analytics providers.
* Review support log export.
* Complete DPIA before production.

## Production Backend Requirements

The mobile app cannot go live alone. The following services must be production ready.

### Issuer

Must provide:

* OpenID4VCI metadata.
* Authorization server metadata.
* Production credential configurations.
* Credential signing keys and certificate chains. Every certificate in a chain below its
  trusted-list entry gives its CRL distribution points and OCSP address as `https://` URLs; the
  wallet does not fetch revocation information over `http://`.
* Wallet/client attestation validation.
* PAR and DPoP behavior according to profile.
* Credential status/revocation support.
* Rate limits and fraud controls.
* Audit logs.
* Incident and key rotation processes.
* A registration certificate the wallet's configured registration trusted list accepts, issued to
  the same registered organization as the issuer's access certificate. An issuer that does not
  present one cannot issue.
* Registered attestation types covering every credential configuration the issuer offers. Offering
  a type outside the registered scope refuses the whole operation, and no app configuration
  overrides that.
* A checkable revocation status for the registration certificate. A status that cannot be
  established counts as a failure, not as a pass.

### Verifier

Must provide:

* OpenID4VP request creation.
* Client identity through X.509, preregistration, or other approved scheme.
* Certificate chains, for request signing and registration, whose certificates below the
  trusted-list entry give their CRL distribution points and OCSP address as `https://` URLs.
* Request signing where required.
* Redirect handling.
* Data minimization by request.
* Trust framework integration.
* User-facing legal name and purpose.
* A registration certificate carried in the request, accepted by the wallet's configured
  registration trusted list, issued to the same registered organization as the certificate that
  signs the request, and with a checkable revocation status. Its contents are shown to the user
  before they share anything: the registered name and identifier, the declared purpose, and the
  privacy policy link. A verifier without one loses the verified indicator and is presented behind
  a warning the user must acknowledge.
* Requested claims kept within the registered scope. Claims beyond it are flagged to the user
  individually and turn the consent screen into an explicit acknowledgement before sharing, which
  depresses completion rates.

### Wallet Provider

Must provide:

* Wallet instance attestation.
* Key attestation (formerly wallet unit attestation).
* App/device risk policy.
* Nonce and replay protection.
* Availability aligned with issuance.
* Monitoring and abuse controls.

### QTSP/RQES

Must provide:

* Production CSC endpoint.
* OAuth/OIDC client registration.
* Redirect URI registration.
* TSA endpoint if required.
* Certificate retrieval trust config.
* Signing policy and audit evidence.

## Build, Install, And Use In Production Testing

### Build

For existing demo release:

```powershell
.\gradlew.bat clean
.\gradlew.bat :app:assembleDemoRelease
```

For recommended production flavor:

```powershell
.\gradlew.bat clean
.\gradlew.bat :app:assembleProdRelease
.\gradlew.bat :app:bundleProdRelease
```

### Install Local Release APK

Use only for internal testing:

```powershell
adb install -r app\build\outputs\apk\prod\release\app-prod-release.apk
```

### Test Core Flows

Before release candidate approval, test:

* First launch.
* PIN creation.
* Login on a device whose screen lock was removed after setup, with `requireDeviceSecure` false and
  true.
* PIN change.
* Biometric enablement (asks for the PIN first) and disablement (PIN or biometric).
* Biometric enrollment change.
* PID issuance from list.
* Credential offer issuance by QR.
* Authorization-code issuance redirect.
* Deferred issuance.
* Reissuance.
* Issuance refused because the provider's registration certificate is absent, invalid, or does not
  cover the offered document.
* Reissuance refused for the same reason, confirming the document already held survives it.
* Revocation.
* Background checks on a new install: no status list is fetched before the first login, and the
  check after login runs once per process.
* Factory reset while re-issuance is completing: its additional revocation request is discarded
  while scheduling is stopped, and dashboard resumes cannot restart checks during deletion.
* Reset timeout or a later reset-step failure: confirm the reset requests periodic revocation and
  re-issuance checks again.
* Successful factory reset: background checks stay stopped until the next login and dashboard visit.
* Re-issue refused for a revoked or suspended document.
* Same-device OpenID4VP presentation.
* Cross-device presentation.
* Remote presentation rejection with and without a redirect. Confirm that **Close** and Android Back
  both exit to the initiating screen, including when the presentation starts during issuance. When a
  redirect is supplied, confirm it opens and returning to the Wallet does not reopen the presentation.
  Confirm that a redirect that is not `https` opens nothing, after a presentation and after a
  rejection.
* Presentation from a verifier with no valid registration certificate, and from one asking beyond
  its registered scope.
* Relayed OpenID4VP request and response handling.
* Proximity QR/BLE, including leaving the app and turning the screen off on the QR screen: the
  session stops and the screen closes.
* NFC engagement, if supported.
* Relayed proximity engagement where feasible.
* RQES signing.
* RQES cancellation.
* Network unavailable.
* Backend unavailable.
* A slow or oversized response from an issuer, verifier or trusted list (fails as a network error).
* Screenshot, screen recording and recents with `shouldPreventScreenCapture` true.
* Expired credential.
* Revoked credential.
* Malformed deep links.
* Malicious credential offer.
* App update from previous production version.

### Verify transaction history in a minified release

Exercise these cases in the release artifact that will be distributed, with R8 enabled:

* Record issuance, reissuance, credential deletion, presentation, and signing activity; restart
  the app and verify that the records remain available and display correctly.
* Exercise QES approval and QES requests with transaction data. Check the details under each
  applicable request option, document links where supplied, and the read-only recorded details
  after restarting the app. Also check that requests without transaction data show no extra section.
* Confirm that updates to the same transaction refresh its existing entry. Delete a pending
  issuance log and verify that a later completion can create the log again, as intended.
* Verify exact-presentation association and communication methods in deletion-request/report
  histories. Confirm that deleting the presentation removes its linked attempts.
* Check external-launch failures and persistence failures separately. Retrying a failed save must
  retain the same attempt and must not reopen the external application.

Verify these behaviors in the minified release; passing debug tests alone is insufficient.

## Production Test Matrix

Minimum device coverage:

| Category | Coverage |
| --- | --- |
| Android versions | API 29 through current target ecosystem. |
| Hardware security | StrongBox available, TEE only, no secure hardware if allowed. |
| Biometric states | None enrolled, weak only, strong enrolled, enrollment changed. |
| Network | Wi-Fi, cellular, captive portal, offline, TLS failure. |
| Locale | All supported Member State languages. |
| Accessibility | Screen reader, font scaling, display size. |
| Permissions | Denied camera, denied Bluetooth, denied location, denied notifications if used. |
| Distribution | Fresh install, update, reinstall, restore attempt. |
| Security | Rooted test device, emulator, debugger, repackaged APK, proxy, relay simulation for remote and proximity presentation flows. |

## Release Evidence Package

For every production release, archive:

* Source commit hash.
* Version name and version code.
* Build environment details.
* Dependency lock or dependency report.
* SBOM.
* SCA report.
* Secret scan report.
* SAST report.
* Lint report.
* Unit and instrumentation test reports.
* MASVS test report.
* Penetration test report or delta assessment.
* Presentation relay-risk threat model and accepted residual risks.
* Verifier policy for age-only, portrait, proximity, and automated presentation scenarios.
* Signed artifact hash.
* Signing certificate fingerprint.
* Mapping files stored securely.
* Privacy review sign-off.
* Change log.
* Rollback and incident plan.

## Pre-Release Automated Checks

Add CI checks that fail if production artifacts contain demo values:

Search for:

```text
eudiw.dev
10.0.2.2
localhost
somesecret
TrustAll
HostnameVerifier { _, _ -> true }
cleartextTrafficPermitted="true"
fallbackToDestructiveMigration(true)
android:debuggable="true"
```

Also check:

* Production manifest has `allowBackup=false`.
* Release variant is not debuggable.
* Release variant is minified.
* Expected app signing certificate is used.
* Expected production application ID is used.
* Expected production issuer and wallet-provider URLs are used.
* No debug-only analytics or crash endpoints are present.

## Operational Readiness

Before going live, define:

* App support model.
* Backend monitoring dashboards.
* Wallet Provider SLA.
* Issuer SLA.
* QTSP SLA.
* Incident response contacts.
* Vulnerability disclosure process.
* Certificate expiry monitoring, registration certificates included — an expired or revoked one
  stops every issuance from that provider.
* Emergency app update process.
* Forced minimum-version process.
* Key compromise process.
* User communication templates.
* Data retention and deletion process.
* Backup and restore policy.
* Audit log access policy.

## Incident Scenarios To Rehearse

Rehearse these before launch:

* Issuer signing key compromise.
* Wallet signing key compromise.
* Wallet Provider outage.
* QTSP outage.
* Production certificate expiry.
* An issuer or verifier removed from the registration trusted list, or its registration certificate
  expired or revoked. For an issuer this stops issuance and re-issuance entirely.
* Bad app release.
* RASP false positive affecting real users.
* Play Integrity verdict degradation.
* Revocation/status service outage.
* Credential metadata mistake.
* Privacy incident from logs or analytics.
* Vulnerability report requiring coordinated disclosure.

## User-Facing Production Behavior

Production UX must clearly explain:

* Why PIN is required.
* Why biometric authentication is optional or required.
* Why camera permission is needed.
* Why Bluetooth/location permissions are needed for proximity.
* What data a verifier is requesting.
* Whether the verifier is trusted, and separately whether it is registered for what it is asking —
  including which requested items fall outside its registration.
* Why a document could not be added, when the provider is not registered for it.
* What happens if a document is revoked or expired.
* What happens if app/device integrity checks fail.
* How users recover access.
* How users delete wallet data.
* How users get support.

Avoid technical error messages for users. Log coarse diagnostic categories separately after redaction.

## Final Go-Live Gate

Do not publish until all of these are true:

* `prodRelease` exists and is the only production artifact.
* No demo URLs or demo secrets remain in production code/resources.
* Production signing is controlled and documented.
* Production trust anchors are installed and documented.
* Issuer and verifier registration certificates validate end-to-end against the production trusted
  lists, and every issuer's registered scope covers what it offers.
* Issuer, verifier, wallet provider, and QTSP integrations pass end-to-end testing.
* Presentation relay-risk controls and residual risks are documented for verifier scenarios.
* MASVS assessment is complete with accepted residual risks.
* RASP/integrity strategy is implemented and tested.
* Privacy and legal reviews are complete.
* Operational monitoring and incident response are ready.
* A rollback or emergency update path exists.

## Reference Links

* EUDI Android Wallet Core: https://github.com/eu-digital-identity-wallet/eudi-lib-android-wallet-core
* EUDI Architecture Reference Framework: https://github.com/eu-digital-identity-wallet/eudi-doc-architecture-and-reference-framework
* OWASP MASVS: https://mas.owasp.org/MASVS/
* OWASP MASTG: https://mas.owasp.org/MASTG/
* OpenID for Verifiable Presentations 1.0: https://openid.net/specs/openid-4-verifiable-presentations-1_0.html
* ISO/IEC 18013-5: https://www.iso.org/standard/69084.html
* Google Play Integrity API: https://developer.android.com/google/play/integrity/overview
* Play Integrity verdicts: https://developer.android.com/google/play/integrity/verdicts
* Android Key Attestation: https://developer.android.com/privacy-and-security/security-key-attestation
* DexGuard: https://www.guardsquare.com/dexguard