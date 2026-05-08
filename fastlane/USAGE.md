# Fastlane Usage

This file documents how to use the Fastlane lanes in this project. The generated lane list remains
in `fastlane/README.md` and may be overwritten by Fastlane.

## Prerequisites

Install Ruby dependencies from the repository root:

```bash
bundle install
```

Run lanes through Bundler:

```bash
bundle exec fastlane android tests
```

The Android build still uses the Gradle wrapper and project configuration described in
[../wiki/how_to_build.md](../wiki/how_to_build.md).

## Common lanes

| Lane | Purpose |
| --- | --- |
| `android tests` | Cleans the project and runs the Kover HTML report task for the selected app build type's debug variant. |
| `android deploy` | Pulls the latest Git state, calculates a build number from Firebase App Distribution, sets version values, builds a release APK, uploads it to Firebase, tags the release, and optionally creates a GitHub release. |
| `android prepare_binary` | Renames the generated release APK to the computed version name. |
| `android upload_firebase` | Uploads the APK to Firebase App Distribution. |
| `android github_release` | Creates a GitHub release and uploads the APK and optional extra attachments. |
| `android set_version` | Writes a CalVer-style `VERSION_NAME` into `version.properties`. |
| `android reset_versioncode` | Resets `versionCode` in `app/build.gradle.kts` to `1`. |
| `android reset_versionName` | Resets `VERSION_NAME` in `version.properties` to `yyyy.mm.v`. |

## Required environment variables

The `deploy` lane expects these values:

| Variable | Required for | Meaning |
| --- | --- | --- |
| `APP_BUILD_TYPE` | Tests and deploy | Flavor/build prefix used in lane task names, for example `Dev` or `Demo`. The lane assembles `assemble${APP_BUILD_TYPE}Release`. |
| `APP_TAG` | Deploy | Tag namespace or brand segment used in the generated Git tag. |
| `FIREBASE_APP_ID` | Firebase upload and build-number lookup | Firebase App Distribution app ID. |
| `FIREBASE_TOKEN` | Firebase upload and build-number lookup | Firebase CLI token or CI secret accepted by the Fastlane Firebase plugin. |
| `FIREBASE_GROUPS` | Firebase upload | Comma-separated Firebase tester groups. |
| `PROD_REMOTE_REPO` | Optional tag upload | Remote URL to push tags to a production mirror. |
| `DEV_REMOTE_REPO` | Optional tag upload | Remote URL to push tags to a development mirror. |
| `GITHUB_RELEASE_REPO` | Optional GitHub release | Repository name for GitHub releases, for example `owner/repo`. |
| `GITHUB_RELEASE_TOKEN` | Optional GitHub release | Token used by Fastlane to create the release. |
| `GITHUB_EXTRA_ATTACHMENTS` | Optional GitHub release | Comma-separated paths to additional release assets. |

Release signing is configured by Gradle, not Fastlane. Current Gradle signing reads:

| Variable/property | Meaning |
| --- | --- |
| `ANDROID_KEY_ALIAS` or `androidKeyAlias` | Release key alias. |
| `ANDROID_KEY_PASSWORD` or `androidKeyPassword` | Release key password. |

For production, store signing values in CI secret storage, HSM/KMS-backed signing, Play App Signing,
or another approved key management process. Do not commit keystores or signing passwords.

## Example commands

Run tests for the `Dev` flavor:

```bash
APP_BUILD_TYPE=Dev bundle exec fastlane android tests
```

Deploy a demo release to Firebase:

```bash
APP_BUILD_TYPE=Demo \
APP_TAG=wallet \
FIREBASE_APP_ID=<firebase-app-id> \
FIREBASE_TOKEN=<firebase-token> \
FIREBASE_GROUPS=<tester-groups> \
bundle exec fastlane android deploy
```

On Windows PowerShell:

```powershell
$env:APP_BUILD_TYPE = "Demo"
$env:APP_TAG = "wallet"
$env:FIREBASE_APP_ID = "<firebase-app-id>"
$env:FIREBASE_TOKEN = "<firebase-token>"
$env:FIREBASE_GROUPS = "<tester-groups>"
bundle exec fastlane android deploy
```

## Release cautions

The current `deploy` lane mutates `version.properties`, temporarily changes `versionCode`, creates a
Git tag, and may push tags to configured remotes. Run it only from a clean working tree and a
controlled CI environment.

Before using Fastlane for production:

* Add a dedicated production flavor, for example `prod`.
* Update the lane to accept and build the production variant intentionally.
* Add secret scanning and demo-endpoint checks before upload.
* Archive the APK/AAB, mapping file, merged manifest, dependency report, SBOM, signing certificate
  fingerprint, Git commit, and test reports.
* Prefer AAB output for Google Play distribution if that is the selected channel.
* Do not rely on Firebase App Distribution build numbers as the only source of production release
  numbering unless the release process explicitly approves it.

For the complete production release process, see [../wiki/go_live.md](../wiki/go_live.md).
