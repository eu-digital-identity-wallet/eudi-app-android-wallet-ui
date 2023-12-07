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

### android demo

```sh
[bundle exec] fastlane android demo
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

### android full_version

```sh
[bundle exec] fastlane android full_version
```

Build Full version

### android tag_name

```sh
[bundle exec] fastlane android tag_name
```

Build Tag Name

### android version_name

```sh
[bundle exec] fastlane android version_name
```

Get version version From Project

### android reset_versioncode

```sh
[bundle exec] fastlane android reset_versioncode
```

Set versionCode back to default 1

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
