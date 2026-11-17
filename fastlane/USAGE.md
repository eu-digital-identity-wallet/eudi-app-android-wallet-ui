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

The lanes build with the Gradle wrapper and the project configuration described in
[../wiki/HOW_TO_BUILD.md](../wiki/HOW_TO_BUILD.md).

## Common lanes

| Lane | Purpose |
| --- | --- |
| `android tests` | Cleans the project and runs the Kover HTML report task for the debug variant of the flavor in `APP_BUILD_TYPE` (for example `koverHtmlReportDevDebug`). |
| `android deploy` | Stops unless every tracked file is committed and unless the BrowserStack settings are complete or absent (`check_browserstack`), then pulls the latest Git state, calculates a build number from Firebase App Distribution, sets version values, builds a release APK, uploads it to Firebase, resets the version values, uploads the APK to BrowserStack App Automate when BrowserStack credentials are set, tags the release, and optionally creates a GitHub release. |
| `android prepare_binary` | Renames the generated release APK to the computed version name. |
| `android upload_firebase` | Uploads the APK to Firebase App Distribution. |
| `android check_browserstack` | Returns `false` when `BROWSERSTACK_USERNAME` and `BROWSERSTACK_ACCESS_KEY` are both empty. Otherwise it stops unless both are set together with `BROWSERSTACK_CUSTOM_ID`, and returns `true`. |
| `android upload_browserstack` | Runs `check_browserstack` and stops when the upload is not configured, then uploads an APK to BrowserStack App Automate with the BrowserStack plugin and the `BROWSERSTACK_` variables: the APK given as `filePath:`, otherwise the release APK of the flavor in `APP_BUILD_TYPE`. Run it on its own to retry an upload that failed in `deploy`. |
| `android release_apk_path` | Returns the path of the release APK of the given flavor, which `upload_browserstack` and `prepare_binary` use. |
| `android github_release` | Creates a GitHub release and uploads the APK and optional extra attachments. |
| `android upload_tag` | Sets the URL of the `origin` remote to the given remote and pushes a Git tag to it. The previous URL is not restored. |
| `android set_version` | Writes a CalVer-style `VERSION_NAME` into `version.properties`. |
| `android reset_versioncode` | Resets `versionCode` in `app/build.gradle.kts` to `1`. |
| `android reset_versionName` | Resets `VERSION_NAME` in `version.properties` to `yyyy.mm.v`. |

## Environment variables

Set the variables in the environment or in an env file: `--env <name>` loads `fastlane/.env.<name>`
(for example `bundle exec fastlane android deploy --env dev`). `.gitignore` covers `.env.dev` and
`.env.demo` only; add any other env file (for example `.env.prod`) to `.gitignore` before writing
tokens into it.

The lanes read these values:

| Variable | Required for | Meaning |
| --- | --- | --- |
| `APP_BUILD_TYPE` | Tests and deploy | Flavor/build prefix used in lane task names, for example `Dev` or `Demo`. The lane assembles `assemble${APP_BUILD_TYPE}Release`. |
| `APP_TAG` | Deploy | Tag namespace or brand segment used in the generated Git tag. |
| `FIREBASE_APP_ID` | Firebase upload and build-number lookup | Firebase App Distribution app ID. |
| `FIREBASE_TOKEN` | Firebase upload and build-number lookup | Firebase CLI token or CI secret accepted by the Fastlane Firebase plugin. |
| `FIREBASE_GROUPS` | Firebase upload | Comma-separated Firebase tester groups. |
| `PROD_REMOTE_REPO` | Optional tag upload | Remote URL to push tags to a production mirror. `upload_tag` sets `origin` to this URL. |
| `DEV_REMOTE_REPO` | Optional tag upload | Remote URL to push tags to a development mirror, after the production one. `upload_tag` sets `origin` to this URL. |
| `GITHUB_RELEASE_REPO` | Optional GitHub release | Repository name for GitHub releases, for example `owner/repo`. |
| `GITHUB_RELEASE_TOKEN` | Optional GitHub release | Token used by Fastlane to create the release. |
| `GITHUB_EXTRA_ATTACHMENTS` | Optional GitHub release | Comma-separated paths to additional release assets. |
| `BROWSERSTACK_USERNAME` | Optional BrowserStack upload | BrowserStack user name. `deploy` skips the BrowserStack upload when this and `BROWSERSTACK_ACCESS_KEY` are both empty. When either is set, `deploy` checks before it builds anything that both are set, with `BROWSERSTACK_CUSTOM_ID`, and stops if one is missing. |
| `BROWSERSTACK_ACCESS_KEY` | Optional BrowserStack upload | BrowserStack access key. Keep it in an env file or CI secret storage, never in Git. With `--env`, an empty line for a key in the env file overrides the value set in the environment; leave the key out of the file when CI provides it. |
| `BROWSERSTACK_CUSTOM_ID` | BrowserStack upload | Required with the credentials. Custom ID given to the upload, for example `eudi-wallet-android`. Tests can name the app by it; it refers to the latest upload with that ID. |

Release signing is configured by Gradle, not Fastlane. Current Gradle signing reads the property from
the root `local.properties` first, else the environment variable:

| Property, else variable | Meaning |
| --- | --- |
| `androidKeyAlias`, else `ANDROID_KEY_ALIAS` | Release key alias. |
| `androidKeyPassword`, else `ANDROID_KEY_PASSWORD` | Release key password. |

The keystore is read from the file `sign` at the repository root, which `.gitignore` excludes. A
Gradle `-P` property is not read.

The current `app/build.gradle.kts` also uses `ANDROID_KEY_PASSWORD` / `androidKeyPassword` for the
keystore `storePassword`. If your release keystore uses a separate store password, update the Gradle
signing config to read a distinct CI secret, for example `ANDROID_KEYSTORE_PASSWORD`.

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

Retry a BrowserStack upload that failed in `deploy`, while the APK is still in place:

```bash
bundle exec fastlane android upload_browserstack --env dev
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

The current `deploy` lane stops when a tracked file has uncommitted changes, so the build and its
tag match `HEAD`, and when the BrowserStack settings are only partly set (`check_browserstack`), before
it pulls or builds anything. It then writes the CalVer `VERSION_NAME` into `version.properties` and the build
number into `versionCode`, builds and uploads the APK to Firebase, and resets both (`yyyy.mm.v`, `1`). When
BrowserStack credentials are set, it then uploads the APK to BrowserStack App Automate; if that
upload fails, the lane stops there: the Firebase release stays without a tag (the next run takes the
next build number), and the APK is kept, so `upload_browserstack` can retry it. Only then does it
create a Git tag and push it to `PROD_REMOTE_REPO` and `DEV_REMOTE_REPO`. To push the tag,
`upload_tag` sets the URL of the `origin` remote to each of them in turn and does not restore it, so
after a run `origin` points at the last of them. Run the lane from a controlled CI environment.

If the lane fails before the reset, the version changes stay in the working tree and the next
`deploy` stops on them; restore them with `git checkout -- version.properties app/build.gradle.kts`
and do not commit them.

Before using Fastlane for production:

* Add a dedicated production flavor, for example `prod`.
* Update the lane to accept and build the production variant intentionally.
* Add secret scanning and demo-endpoint checks before upload.
* Archive the APK/AAB, mapping file, merged manifest, dependency report, SBOM, signing certificate
  fingerprint, Git commit, and test reports.
* Prefer AAB output for Google Play distribution if that is the selected channel.
* Do not rely on Firebase App Distribution build numbers as the only source of production release
  numbering unless the release process explicitly approves it.

For the complete production release process, see [../wiki/GO_LIVE.md](../wiki/GO_LIVE.md).