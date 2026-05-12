# EUDI Android Wallet Go-Live Guide

This guide explains how an integrator, implementer, or Member State team can take this Android
reference wallet and turn it into a production candidate.

It complements:

* [How to build](how_to_build.md)
* [How to configure the application](configuration.md)
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

## Go-Live Checklist

Use this checklist before the first production release.

| Area | Required production outcome |
| --- | --- |
| App identity | Final application ID, app name, icon, signing key, Play Console or alternative distribution identity are defined. |
| Build variants | A dedicated production flavor exists, for example `prodRelease`; `dev` and `demo` remain non-production only. |
| Signing | Release keys are generated, stored in HSM/KMS or CI secret storage, rotated according to policy, and never committed. |
| Issuers | All OpenID4VCI issuer URLs point to production issuer services controlled or approved by the implementer. |
| Wallet provider | `walletProviderHost` points to the production Wallet Provider service and supports the expected attestation endpoints. |
| Trust anchors | Demo and development certificates are replaced by production IACA/reader/verifier trust anchors. |
| RQES | QTSP, TSA, client ID, redirect URI, and certificate retrieval settings are production values. |
| Secrets | No production secret is hardcoded in Kotlin, Gradle, resources, or `BuildConfig`. |
| Network | Cleartext traffic is disabled; trust-all certificate logic is absent; TLS policy and certificate pinning strategy are agreed. |
| Storage | Wallet data, database keys, PIN material, and logs are protected, excluded from backup, and migration-safe. |
| Authentication | PIN, biometrics, device credential fallback, key authentication, lockout, and recovery policies are approved. |
| Deep links | All inbound URI schemes, hosts, and parameters are validated and threat-modeled. |
| Logs | Production logs do not include PID, credentials, tokens, request objects, signatures, keys, or user decisions. |
| RASP | Play Integrity, commercial protection such as DexGuard, or a manual RASP strategy is implemented. |
| MASVS | Controls are mapped to evidence and tested with static, dynamic, and manual security testing. |
| Privacy | DPIA, data minimization, retention, telemetry, consent, and privacy notice are complete. |
| Operations | Monitoring, incident response, vulnerability disclosure, certificate rotation, and forced update processes exist. |

## Current Project Shape

The application is a modular Android project. Important modules for production are:

| Module                       | Production relevance                                                                                                                   |
|------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `app`                        | Application ID, signing, release build type, baseline profile packaging.                                                               |
| `assembly-logic`             | Android application class, manifest, permissions, activities, deep links, NFC service, app name.                                       |
| `core-logic`                 | Wallet Core integration, issuer configuration, wallet provider, trust stores, document issuance, presentation, revocation, reissuance. |
| `business-logic`             | Global app config, RQES config, logging, encrypted preference storage, crypto helpers.                                                 |
| `authentication-logic`       | PIN and biometric storage providers and controllers.                                                                                   |
| `storage-logic`              | SQLCipher-backed Room database and local transaction/revocation storage.                                                               |
| `network-logic`              | Ktor client, logging level, wallet attestation repository.                                                                             |
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

| File | Why it matters |
| --- | --- |
| `build-logic/convention/src/main/kotlin/project/convention/logic/AppFlavor.kt` | Defines product flavors and application ID suffixes. |
| `app/build.gradle.kts` | Defines app ID, signing config, release minification, version code. |
| `assembly-logic/build.gradle.kts` | Defines app display name placeholder. |
| `assembly-logic/src/main/AndroidManifest.xml` | Defines exported components, permissions, backups, deep links, NFC service, network security config. |
| `core-logic/src/<flavor>/java/eu/europa/ec/corelogic/config/WalletCoreConfigImpl.kt` | Main wallet network, trust, issuer, document, and presentation configuration. |
| `business-logic/src/<flavor>/java/eu/europa/ec/businesslogic/config/ConfigLogicImpl.kt` | Flavor identity, changelog URL, app behavior toggles. |
| `business-logic/src/<flavor>/java/eu/europa/ec/businesslogic/config/RQESConfigImpl.kt` | RQES QTSP/TSA/client config and document retrieval trust config. |
| `network-logic/src/main/java/eu/europa/ec/networklogic/di/NetworkModule.kt` | Ktor client behavior and release logging. |
| `network-logic/src/main/res/xml/network_security_config.xml` | Cleartext and certificate trust policy. |
| `business-logic/src/main/res/xml/backup_rules.xml` | Auto Backup policy for Android 11 and below. |
| `business-logic/src/main/res/xml/data_extraction_rules.xml` | Cloud backup and device transfer policy for Android 12 and above. |
| `authentication-logic/src/main/java/eu/europa/ec/authenticationlogic/storage/PrefsPinStorageProvider.kt` | PIN hashing, salt, iteration count, constant-time comparison. |
| `business-logic/src/main/java/eu/europa/ec/businesslogic/controller/storage/PrefsController.kt` | Encrypted DataStore and database key storage. |
| `storage-logic/src/main/java/eu/europa/ec/storagelogic/di/LogicStorageModule.kt` | SQLCipher database setup and migration behavior. |

## Create A Production Flavor

Do not reuse `demo` for production. Add a new flavor, for example `prod`, so production values are
isolated and reviewable.

### 1. Add `Prod` To `AppFlavor`

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

| Setting | Current location | Production guidance |
| --- | --- | --- |
| `applicationId` | `app/build.gradle.kts` | Use a final reverse-DNS ID owned by the implementer, for example `eu.example.wallet`. Changing it after release creates a different app. |
| `namespace` | `app/build.gradle.kts` and modules | Can remain internal package names, but align with your code ownership and policy. |
| App name | `assembly-logic/build.gradle.kts`, manifest placeholder `appName` | Use the official wallet name approved for the Member State or wallet provider. |
| App icon | `assembly-logic/src/main/AndroidManifest.xml`, mipmap resources | Replace reference icons with production brand assets. |
| Version name | `version.properties` | Replace `yyyy.mm.v` with your release versioning scheme. |
| Version code | `app/build.gradle.kts` or CI mutation | Must monotonically increase for app stores. |
| Min SDK | `build-logic/.../KotlinAndroid.kt` | Current `minSdk` is 29. Confirm with your device support policy. |
| Target SDK | `build-logic/.../AndroidApplicationConventionPlugin.kt` | Current `targetSdk` is 36. Keep current with Android policy. |

Recommended:

* Freeze `applicationId` before beta testing with real users.
* Keep `dev` and `demo` app IDs separate with suffixes.
* Ensure app display names make non-production builds obvious.
* Maintain a release manifest showing version name, version code, Git commit, signing certificate
  fingerprint, and dependency versions.

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

## Dependency Versions

The central version catalog is:

`gradle/libs.versions.toml`

At the time this guide was written, key versions included:

| Dependency | Current repo version |
| --- | --- |
| Android Gradle Plugin | `9.2.1` |
| Kotlin | `2.3.21` |
| EUDI Wallet Core | `0.27.1` |
| EUDI RQES UI SDK | `0.3.8` |
| Ktor | `3.4.3` |
| SQLCipher Android | `4.15.0` |
| OWASP Dependency Check | `12.2.2` |

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
}
```

Each property must be reviewed.

## `EudiWalletConfig`

Current flavor implementations configure:

* Document key creation.
* OpenID4VP.
* Digital Credential API.
* Reader trust store.

### Document Key Creation

Current code:

```kotlin
configureDocumentKeyCreation(
    userAuthenticationRequired = false,
    userAuthenticationTimeout = 30.seconds,
    useStrongBoxForKeys = true
)
```

Meaning:

| Value | Meaning | Production guidance |
| --- | --- | --- |
| `userAuthenticationRequired` | Whether document private keys require device user authentication before use. | For high-assurance credentials, strongly consider `true`. If `false`, the app-level PIN protects flows, but document keys are not individually bound to a platform authentication event. |
| `userAuthenticationTimeout` | Time window after authentication during which key use remains allowed. | Use the shortest practical value. For very sensitive operations, use `0.seconds` or an equivalent per-use requirement if supported and acceptable for UX. |
| `useStrongBoxForKeys` | Requests StrongBox-backed key storage where available. | Keep `true`. Define fallback behavior for devices without StrongBox. Decide whether such devices are allowed, restricted, or rejected. |

Recommended production example:

```kotlin
configureDocumentKeyCreation(
    userAuthenticationRequired = true,
    userAuthenticationTimeout = 30.seconds,
    useStrongBoxForKeys = true
)
```

Decision point:

* If the Member State requires a high assurance level for PID presentation or signing-related
  actions, document key use should be bound to strong local authentication.
* If the supported device population lacks StrongBox, define a policy for hardware-backed TEE keys
  versus StrongBox-only devices.

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

### Digital Credential API

Current code:

```kotlin
configureDCAPI {
    withEnabled(true)
}
```

Production guidance:

* Keep enabled only if your product supports Android Digital Credential API flows.
* Test with all supported Android versions and browsers.
* Validate all inbound DC API intents.
* Confirm policy implications for relying parties and browser mediation.
* Include DC API flows in MASVS platform-interaction testing.

### Reader Trust Store

Current code loads PEM files from:

```text
resources-logic/src/main/res/raw
```

The current resources include development/demo trust anchors. In production:

* Remove trust anchors that are not part of the production trust framework.
* Add production IACA, reader root, verifier, or scheme certificates according to your trust model.
* Give files clear names, for example `ms_iaca_2026.pem`.
* Document certificate owner, purpose, fingerprint, validity, and rotation plan.
* Add tests that verify expected production trust anchors are present and demo anchors are absent.

Example:

```kotlin
configureReaderTrustStore(
    context,
    R.raw.ms_iaca_2026,
    R.raw.ms_reader_root_2026
)
```

Certificate governance:

* Store source PEMs in version control only if policy allows public trust anchors there.
* Do not store private keys in the app repository.
* Define rotation before expiry.
* Define emergency distrust and app update procedures.

## Issuer Configuration: `issuersConfig`

Current demo code contains issuer URLs such as:

```kotlin
.withIssuerUrl(issuerUrl = "https://issuer.eudiw.dev")
.withIssuerUrl(issuerUrl = "https://issuer-backend.eudiw.dev")
```

Production values must point to your production OpenID4VCI issuers.

Example:

```kotlin
override val issuersConfig: List<VciConfig>
    get() = listOf(
        VciConfig(
            config = OpenId4VciManager.Config.Builder()
                .withIssuerUrl(issuerUrl = "https://issuer.pid.example.eu")
                .withClientAuthenticationType(
                    OpenId4VciManager.ClientAuthenticationType.AttestationBased
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
| `issuerUrl` | Base URL of an OpenID4VCI credential issuer. | Your production issuer base URL. It must expose valid issuer metadata and authorization metadata. |
| `ClientAuthenticationType.AttestationBased` | Wallet authenticates with wallet/key attestation. | Use if your issuer requires wallet instance or wallet unit attestation. Align with Wallet Provider. |
| `withAuthFlowRedirectionURI` | Redirect URI for authorization-code issuance flow. | Must exactly match the app manifest and the issuer client registration. |
| `withParUsage` | Pushed Authorization Request use. | Prefer `REQUIRED` if issuer mandates PAR. `IF_SUPPORTED` is acceptable for interoperability where issuer policy allows it. |
| `DPopConfig.Default` | Enables default DPoP behavior for proof-of-possession. | Keep enabled unless your issuer profile explicitly does not support it. |
| `order` | Display order in the "Add document" list. | Use stable order. Put PID issuer first if PID activation is required. |

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
/wallet-unit-attestation/jwk-set
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
/wallet-unit-attestation/platform-key-attestation/android
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
the same `walletInstanceAttestation` / `walletUnitAttestation` response fields after backend
validation. Do not only configure the backend and leave the app calling the JWK endpoints.

Production endpoints must also validate wallet application integrity, not only key provenance. Use
Play Integrity API, a national/app-store equivalent, commercial RASP attestation, or another
server-verified integrity mechanism to bind wallet attestations to the expected package name,
signing certificate, installer/distribution channel, app version, device integrity state, and a
fresh backend nonce.

The production Wallet Provider must:

* Use attestation-backed endpoints for production wallet instance and wallet unit attestations.
* Treat plain-JWK endpoints as non-production unless additional server-side validation makes them
  equivalent to the production attestation policy.
* Validate wallet app, device, and key evidence according to your policy.
* Return `walletInstanceAttestation` and `walletUnitAttestation` values in the expected response fields.
* Use HTTPS with production TLS.
* Rate-limit requests.
* Detect abuse and replay.
* Log enough for audit without storing unnecessary personal data.
* Have availability targets aligned with issuance and reissuance flows.

Do not point production builds to EUDI demo wallet-provider services.

## Document Issuance Rules

Current code:

```kotlin
DocumentIssuanceConfig(
    defaultRule = DocumentIssuanceRule(
        policy = CredentialPolicy.RotateUse,
        numberOfCredentials = 1
    ),
    documentSpecificRules = mapOf(
        DocumentIdentifier.MdocPid to DocumentIssuanceRule(
            policy = CredentialPolicy.OneTimeUse,
            numberOfCredentials = 10
        ),
        DocumentIdentifier.SdJwtPid to DocumentIssuanceRule(
            policy = CredentialPolicy.OneTimeUse,
            numberOfCredentials = 10
        ),
    ),
    reissuanceRule = ReIssuanceRule(
        minNumberOfCredentials = 2,
        minExpirationHours = 24,
        backgroundInterval = Duration.ofMinutes(15)
    )
)
```

Meaning:

| Field | Meaning | Production decision |
| --- | --- | --- |
| `CredentialPolicy.RotateUse` | Reuse/rotate credentials according to Wallet Core behavior. | Use for documents where a single reusable credential is acceptable. |
| `CredentialPolicy.OneTimeUse` | Issue a batch so each presentation can consume a credential. | Use for unlinkability/privacy-sensitive credentials such as PID where required. |
| `numberOfCredentials` | Number of credentials requested at issuance. | Balance privacy, user offline needs, issuer load, and storage size. |
| `minNumberOfCredentials` | Background reissuance starts when remaining credentials are at or below this count. | Set high enough to avoid users running out. |
| `minExpirationHours` | Reissue when credentials expire within this window. | Set according to credential validity and issuer SLA. |
| `backgroundInterval` | WorkManager interval for reissuance checks. | Android periodic work minimum is effectively 15 minutes. Avoid excessive issuer load. |

Production questions:

* Which credential types require one-time-use?
* How many offline presentations must users be able to perform?
* What happens if reissuance fails for days?
* Is authorization fallback allowed for background reissuance?
* Are users informed when documents are close to exhaustion or expiry?
* Does the issuer rate-limit batch issuance?

Recommended:

* Keep PID and privacy-sensitive credentials as one-time-use if required by the profile.
* Start with a conservative batch size and tune after load testing.
* Monitor reissuance failures.
* Do not silently delete or replace user credentials without clear UX and audit behavior.

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

Current default:

```kotlin
override val revocationInterval: Duration get() = Duration.ofMinutes(15)
```

The app enqueues `RevocationWorkManager`, which:

* Loads issued documents.
* Resolves document status through Wallet Core.
* Stores invalid or suspended document IDs.
* Broadcasts changes to refresh UI.

Production decisions:

* Confirm the status mechanism used by each credential type.
* Define expected behavior when status checking fails.
* Define whether invalid/suspended documents are hidden, marked, blocked, or deleted.
* Define battery/network policy.
* Define whether users can manually refresh status.
* Include revocation in incident response and issuer operations.

Recommended:

* Keep background checks, but add observability around repeated failures.
* Add UX for "status could not be refreshed" if required.
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

| Field | Meaning | Production value |
| --- | --- | --- |
| `name` | QTSP display name. | Official QTSP/service name shown to users. |
| `endpoint` | CSC or QTSP signing endpoint. | Production HTTPS endpoint from the QTSP. |
| `tsaUrl` | Timestamp authority URL. | Approved TSA endpoint, if required by the signing profile. |
| `clientId` | OAuth/client identifier for the wallet or broker. | Production client ID issued by QTSP or authorization server. |
| `clientSecret` | OAuth client secret in current SDK config. | Avoid embedding real confidential secrets in the app. Use a backend broker or public-client profile where possible. |
| `authFlowRedirectionURI` | Redirect URI for RQES authorization. | Must match manifest placeholders and QTSP registration. |
| `hashAlgorithm` | Hash algorithm for signing. | Use approved algorithm, currently SHA-256 in the reference config. |
| `documentRetrievalConfig` | Certificate/trust config for retrieving documents. | Replace demo certificates with production trusted certificates. |

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
| Internal wallet navigation | `eudi-wallet` |
| OpenID4VP | `openid4vp`, `eudi-openid4vp`, `mdoc-openid4vp`, `haip-vp` |
| Credential offers | `openid-credential-offer`, `haip-vci` |
| OpenID4VCI authorization redirect | `eu.europa.ec.euidi://authorization` |
| RQES OAuth redirect | `rqes://oauth/callback` |
| RQES document retrieval | `eudi-rqes` |

Production concerns:

* Many manifest filters use wildcard hosts (`*`).
* Custom URL schemes can be claimed by other apps.
* Incoming URIs are external input and must be treated as untrusted.
* Redirect URI values must exactly match issuer/QTSP client registration.

Production rules:

* Keep only schemes required by your supported protocols.
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
| `HIDE_OVERLAY_WINDOWS` | Requested | Keep only if overlay protection is part of your UX/security design. |
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
* All issuer, verifier, wallet provider, QTSP, TSA, status, analytics, and telemetry endpoints must
  use HTTPS.
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

Production guidance:

* Keep encrypted DataStore for low-volume sensitive preferences.
* Exclude DataStore files and Tink keysets from backup and device transfer.
* Define behavior when the Android Keystore key is unavailable or invalidated.
* Avoid storing long-lived tokens unless strictly required.
* Do not store PID attributes in preferences.

### PIN Storage

`PrefsPinStorageProvider` stores:

* Random 32-byte salt.
* PBKDF2-HMAC-SHA256 hash.
* 210,000 iterations.
* Constant-time hash comparison.
* Encrypted preference storage through `PrefsController`.

Production gaps to address:

* Add failed-attempt counting.
* Add exponential backoff or lockout.
* Define maximum attempts before reset/recovery.
* Consider binding PIN verification to a hardware-backed key operation.
* Benchmark PBKDF2 iterations on the slowest supported device.
* Consider memory-hard KDF options if supported by your platform/security policy.
* Add telemetry for repeated failed attempts without logging PIN values.

Example policy:

| Event | Suggested behavior |
| --- | --- |
| 5 failed attempts | Delay next attempt by 30 seconds. |
| 10 failed attempts | Require device credential or stronger step-up. |
| 15 failed attempts | Lock wallet until recovery or re-enrollment. |
| App reinstall | Require re-enrollment unless secure backup/recovery is explicitly implemented. |

Do not implement lockout only in UI state. Persist counters securely and make bypass harder with
RASP and server-side risk checks where applicable.

### SQLCipher Room Database

`LogicStorageModule` uses SQLCipher through:

```kotlin
.openHelperFactory(SupportOpenHelperFactory(key))
```

The database stores:

* Bookmarks.
* Revoked document IDs.
* Transaction logs.
* Failed reissuance document IDs.

Current risk:

```kotlin
.fallbackToDestructiveMigration(true)
```

Production guidance:

* Replace destructive migration with explicit Room migrations.
* Add migration tests.
* Define retention for transaction logs.
* Minimize transaction log content.
* Review whether transaction logs contain personal data or verifier details.
* Wipe database key material from memory where practical after opening the DB.
* Exclude DB files from backup and device transfer.

Example:

```kotlin
Room.databaseBuilder(
    context,
    DatabaseService::class.java,
    "eudi.app.wallet.storage"
)
    .openHelperFactory(SupportOpenHelperFactory(key))
    .addMigrations(MIGRATION_2_3)
    .build()
```

## Logging And Audit

The app has:

* Network logging disabled in release.
* Wallet Core logging routed through `WalletCoreLogController`.
* File logging through `LogControllerImpl`.
* Transaction logging through `WalletCoreTransactionLogControllerImpl`.

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

Current `LogControllerImpl` always plants a `Timber.DebugTree()` and a file logger. For production,
consider:

* Planting a production-safe tree only for release.
* Raising minimum file log priority.
* Removing file logging entirely unless support policy requires it.
* Adding a redacting logger facade.
* Making log export explicit and user-controlled.

Example production-safe pattern:

```kotlin
if (configLogic.appBuildType == AppBuildType.DEBUG) {
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
| Session duration | Keep short. Reauthenticate for disclosure and signing. |
| PIN length | Current UI defaults to 6 digits. Confirm policy. |
| Recovery | Define what happens if user forgets PIN or changes biometrics. |

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
* Consider enabling R8 full mode after compatibility testing. The repo currently sets
  `android.enableR8.fullMode=false`.

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

* Add PIN attempt lockout.
* Decide device credential fallback policy.
* Consider `userAuthenticationRequired = true` for document keys.
* Test biometric enrollment changes.
* Test authentication bypass attempts.

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
* Analytics avoids personal data.
* Retention is defined.
* Logs and transaction history are privacy-reviewed.
* Permissions are justified.
* Privacy notice is accurate.

Project-specific actions:

* Review presentation request UI for clarity.
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
* Credential signing keys and certificate chains.
* Wallet/client attestation validation.
* PAR and DPoP behavior according to profile.
* Credential status/revocation support.
* Rate limits and fraud controls.
* Audit logs.
* Incident and key rotation processes.

### Verifier

Must provide:

* OpenID4VP request creation.
* Client identity through X.509, preregistration, or other approved scheme.
* Request signing where required.
* Redirect handling.
* Data minimization by request.
* Trust framework integration.
* User-facing legal name and purpose.

### Wallet Provider

Must provide:

* Wallet instance attestation.
* Wallet unit/key attestation.
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
* PIN change.
* Biometric enablement.
* Biometric enrollment change.
* PID issuance from list.
* Credential offer issuance by QR.
* Authorization-code issuance redirect.
* Deferred issuance.
* Reissuance.
* Revocation.
* Same-device OpenID4VP presentation.
* Cross-device presentation.
* Proximity QR/BLE.
* NFC engagement, if supported.
* RQES signing.
* RQES cancellation.
* Network unavailable.
* Backend unavailable.
* Expired credential.
* Revoked credential.
* Malformed deep links.
* Malicious credential offer.
* App update from previous production version.

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
| Security | Rooted test device, emulator, debugger, repackaged APK, proxy. |

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
* Certificate expiry monitoring.
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
* Whether the verifier is trusted.
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
* Issuer, verifier, wallet provider, and QTSP integrations pass end-to-end testing.
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
* Google Play Integrity API: https://developer.android.com/google/play/integrity/overview
* Play Integrity verdicts: https://developer.android.com/google/play/integrity/verdicts
* Android Key Attestation: https://developer.android.com/privacy-and-security/security-key-attestation
* DexGuard: https://www.guardsquare.com/dexguard
