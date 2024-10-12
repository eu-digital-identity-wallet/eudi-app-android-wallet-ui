fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android tests

```sh
[bundle exec] fastlane android tests
```

Runs all the unit tests

### android deploy

```sh
[bundle exec] fastlane android deploy
```

Build Wallet and upload it to appcenter

### android upload_to_appcenter

```sh
[bundle exec] fastlane android upload_to_appcenter
```

Upload to AppCenter

### android latest_appcenter_release

```sh
[bundle exec] fastlane android latest_appcenter_release
```

Get Release version From AppCenter

### android build_number

```sh
[bundle exec] fastlane android build_number
```

Build Number

### android tag_name

```sh
[bundle exec] fastlane android tag_name
```

Build Tag Name

### android full_version

```sh
[bundle exec] fastlane android full_version
```

Get version version From Project

### android reset_versioncode

```sh
[bundle exec] fastlane android reset_versioncode
```

Set versionCode back to default 1

### android reset_versionName

```sh
[bundle exec] fastlane android reset_versionName
```

Set versionName back to default yyyy.d.m

### android set_version

```sh
[bundle exec] fastlane android set_version
```

Build VersionName based on CalVer

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
