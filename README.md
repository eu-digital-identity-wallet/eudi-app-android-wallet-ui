# EUDI Android Wallet reference application

:heavy_exclamation_mark: **Important!** Before you proceed, please read
the [EUDI Wallet Reference Implementation project description](https://github.com/eu-digital-identity-wallet/.github/blob/main/profile/reference-implementation.md)

----

## Table of contents

* [Overview](#overview)
* [Disclaimer](#disclaimer)
* [Important things to know](#important-things-to-know)
* [How to contribute](#how-to-contribute)
* [Demo videos](#demo-videos)
* [How to use the application](#how-to-use-the-application)
* [How to build - Quick start guide](#how-to-build---quick-start-guide)
* [Application configuration](#application-configuration)
* [License](#license)

## Overview

The EUDI Wallet Reference Implementation is built based on the [Architecture Reference Framework](https://github.com/eu-digital-identity-wallet/eudi-doc-architecture-and-reference-framework/blob/main/docs/architecture-and-reference-framework-main.md) and aims to showcase a robust and interoperable platform for digital identification, authentication, and electronic signatures based on common standards across the European Union.
The EUDI Wallet Reference Implementation is based on a modular architecture composed of business-agnostic, reusable components that will evolve in incremental steps and can be re-used across multiple projects.

The EUDI Wallet Reference Implementation is the application that allows users to:

1. To obtain, store, and, present PID and mDL.
2. Verify presentations.
3. Share data on proximity scenarios.
4. Support remote QES and more use cases with the modules included.

The EUDIW project provides through this repository an Android app. Please refer to the repositories listed in the following sections for more detailed information on how to get started, contribute, and engage with the EUDI Wallet Reference Implementation.
 
# 💡 Specifications Employed

The app consumes the SDK called EUDIW Wallet core [Wallet core](https://github.com/eu-digital-identity-wallet/eudi-lib-android-wallet-core) and a list of available libraries to facilitate remote presentation, proximity, and issuing test/demo functionality following specification of the [ARF](https://github.com/eu-digital-identity-wallet/eudi-doc-architecture-and-reference-framework) including:
 
- OpenID4VP - draft 19 (remote presentation), presentation exchange v2.0,
 
- ISO18013-5 (proximity presentation),
 
- OpenID4VCI draft 13 (issuing)
 
- Issuer functionality, to support development and testing, one can access an OID4VCI test/demo service for issuing at: 

  - [EUDI Issuer (Draft 13)](https://issuer.eudiw.dev/)

  - [OpenID4VCI PID and mDL Issuer (python)](https://github.com/eu-digital-identity-wallet/eudi-srv-web-issuing-eudiw-py)
 
  - [OpenID4VCI PID and mDL Issuer (kotlin)](https://github.com/eu-digital-identity-wallet/eudi-srv-pid-issuer)
 
Relying Party functionality:
 
To support development and testing, one can access a test/demo service for remote presentation at:

  - [EUDI Verifier](https://verifier.eudiw.dev) 

  - [Web verifier source](https://github.com/eu-digital-identity-wallet/eudi-web-verifier)

  - [Verifier restful backend service source](https://github.com/eu-digital-identity-wallet/eudi-srv-web-verifier-endpoint-23220-4-kt)
 
To support proximity an Android Proximity Verifier is available as an app that can request PID and mDL with reader authentication available [here](https://install.appcenter.ms/orgs/eu-digital-identity-wallet/apps/mdoc-verifier-testing/distribution_groups/eudi%20verifier%20(testing)%20public)

The issuer, verifier service, and verifier app authentication are based on the EUDIW development [IACA](https://github.com/niscy-eudiw/eudi-app-android-wallet-ui/tree/main/resources-logic/src/main/res/raw)

## Important things to know

The main purpose of the reference implementation is to showcase the ecosystem and act as a technical example of how to integrate and use all of the available components.

If you're planning to use this application in production, we recommend reviewing the following steps:
- Configure the application properly by following the guide [here](wiki/configuration.md)
- Ensure the Pin storage configuration matches your security requirements or provide your own by following this guide [Pin Storage Configuration](wiki/configuration.md#pin-storage-configuration)
- Ensure the application meets the OWASP MASVS industry standard. Please refer to the following links for further information on the controls you must implement to ensure maximum compliance:
    - [OWASP MASVS](https://mas.owasp.org/MASVS/)
    - [Play Integrity API](https://developer.android.com/google/play/integrity)

## How to contribute

We welcome contributions to this project. To ensure that the process is smooth for everyone
involved, follow the guidelines found in [CONTRIBUTING.md](CONTRIBUTING.md).

## Demo videos

Issuance

[Issuance](https://github.com/eu-digital-identity-wallet/eudi-app-android-wallet-ui/assets/129499766/60732c14-653a-46d5-a87a-8973f8823d0f)

Presentation

[Presentation](https://github.com/eu-digital-identity-wallet/eudi-app-android-wallet-ui/assets/129499766/21050222-2c07-4bcd-983b-4f6d4cf20248)

Proximity

[Proximity](https://github.com/eu-digital-identity-wallet/eudi-app-android-wallet-ui/assets/129499766/c92f1818-e64c-463d-98c5-4f9f87c61760)

## How to use the application

### Minimum device requirements

- API level 28.

### Prerequisites

You can download the application [here](https://install.appcenter.ms/orgs/eu-digital-identity-wallet/apps/eudi-reference-android/distribution_groups/eudi%20wallet%20(demo)%20public)

You will also need to download the Android Verifier app. More information can be found [here](wiki/verifier_proximity.md)

### App launch

1. Launch the application
2. You will be presented with a welcome screen where you will be asked to create a PIN for future logins.

### Issuance flow (Scoped - Wallet initiated)

1. Go to the "Dashboard" screen -> "Documents" tab and tap on the "+" icon (top-right of the screen) and select the "From list" option, or if it's the first time you open the app, you will be redirected there after you enter or set up your PIN.
2. Pick "PID".
3. From the web view that appears select the "Country Selection" option, then "FormEU" and tap submit.
4. Fill in the form. Any data will do, but it is recommended to select a birth-date that is >18 years old, as it is required for some functionality of the app(e.g. RQES).
5. You will be shown a success screen. Tap "Close".
6. You are now on the "Dashboard" screen, and depending on if it was your first Document added on the Wallet, you will be either on the "Home" tab (if it was), or on the "Documents" tab. The flow is complete.

### Issuance flow (Credential Offer - Issuer initiated)

1. Go to the "Dashboard" screen -> "Documents" tab and tap on the "+" icon (top-right of the screen) and select the "Scan QR" option.
2. Scan The QR Code from the issuer's website [EUDI Issuer](https://issuer.eudiw.dev/credential_offer_choice)
3. Review the documents contained in the credential offer and tap "Add".
4. From the web view that appears select the "Country Selection" option, then "FormEU" and tap submit.
5. Fill in the form. Any data will do, but it is recommended to select a birth-date that is >18 years old, as it is required for some functionality of the app(e.g. RQES).
6. You will be shown a success screen. Tap "Close".
7. You are back on the "Documents" tab (of the "Dashboard" screen). The flow is complete.

If you want to delete a document, you can do so by tapping on it in the "Documents" tab (of the "Dashboard" screen) and tapping the "Delete document" button in the "Document details" screen.

### Presentation (Online authentication/Same device) flow.

1. Go to the browser application on your device and enter "https://verifier.eudiw.dev"
2. Expand the Person Identification Data (PID) card and select "attributes by" -> "Specific attributes" and "format" -> the format of your choice.
3. Press next and then "Select Attributes"
4. Pick the fields you want to request from the Wallet (e.g. "Family Name" and "Given Name")
5. Review your presentation request, click next and then tap on the "Open with your Wallet" button.
6. When asked to open the wallet app tap "Open".
7. You will be returned to the app's "Request" screen. Here you can select/deselect to share with the Verifier any of the attributes they request. You must have selected at least one attribute in order to proceed.
8. Tap "Share."
9. Enter the PIN you added in the initial steps.
10. On success tap "Close".
11. A browser will open showing that the Verifier has accepted your request.
12. Return to the app. The flow is complete.

### Proximity flow

1. Log in to the EUDI Wallet app.
2. You are now on the "Home" tab (of the "Dashboard" screen).
3. Tap on the 'Authenticate' button of the first informative card and a modal with 2 options is presented.
4. Select the "In person" option.
5. You will be requested to have your Bluetooth enabled (if it is not already enabled), and to allow permission to the app to use it (if you have not already done it).
6. The Verifier scans the presented QR code.
7. The app's "Request" screen will be loaded. Here you can select/deselect to share with the Verifier any of the attributes they request. You must have selected at least one attribute in order to proceed.
8. Tap "Share."
9. Enter the PIN you added in the initial steps.
10. On success tap "Close".
11. The Verifier will receive the data you chose to share with.
12. You are back on the "Home" tab (of the "Dashboard" screen). The flow is complete.

## How to build - Quick start guide

[This document](wiki/how_to_build.md) describes how you can build the application and deploy the issuing and verification services locally.

## Application configuration

You can find instructions on how to configure the application [here](wiki/configuration.md)

## Disclaimer

The released software is an initial development release version: 
-  The initial development release is an early endeavor reflecting the efforts of a short time-boxed period, and by no means can be considered as the final product.  
-  The initial development release may be changed substantially over time, might introduce new features but also may change or remove existing ones, potentially breaking compatibility with your existing code.
-  The initial development release is limited in functional scope.
-  The initial development release may contain errors or design flaws and other problems that could cause system or other failures and data loss.
-  The initial development release has reduced security, privacy, availability, and reliability standards relative to future releases. This could make the software slower, less reliable, or more vulnerable to attacks than mature software.
-  The initial development release is not yet comprehensively documented. 
-  Users of the software must perform sufficient engineering and additional testing to properly evaluate their application and determine whether any of the open-sourced components are suitable for use in that application.
-  We strongly recommend not putting this version of the software into production use.
-  Only the latest version of the software will be supported

## Package structure

*assembly-logic*: App dependencies.

*build-logic*: Application gradle plugins.

*resources-logic*: All app resources reside here (images, etc.)

*analytics-logic*: Access to analytics providers. Capabilities for test monitoring analytics (i.e. crashes) can be added here (no functionality right now)

*business-logic*: App business logic.

*core-logic*: Wallet core logic.

*storage-logic*: Persistent storage cache.

*authentication-logic*: Pin/Biometry Storage and System Biometrics Logic.

*ui-logic*: Common UI components.

*common-feature*: Code that is common to all features.

*dashboard-feature*: The application main screen.

*startup-feature*: The initial screen of the app.

*presentation-feature*: Online authentication feature.

*issuance-feature*: Document issuance feature.

*proximity-feature*: Proximity scenarios feature.


```mermaid
graph TD;
  startup-feature --> assembly-logic
  dashboard-feature --> assembly-logic
  presentation-feature --> assembly-logic
  proximity-feature --> assembly-logic
  issuance-feature --> assembly-logic

  common-feature --> startup-feature
  common-feature --> dashboard-feature
  common-feature --> presentation-feature
  common-feature --> proximity-feature
  common-feature --> issuance-feature

  business-logic -->common-feature
  ui-logic -->common-feature
  network-logic -->common-feature
  resources-logic -->common-feature
  analytics-logic -->common-feature 
  authentication-logic -->common-feature 
  core-logic -->common-feature
  storage-logic -->common-feature 

  business-logic -->core-logic
  resources-logic -->core-logic
  authentication-logic -->core-logic

  business-logic -->ui-logic
  resources-logic -->ui-logic
  analytics-logic -->ui-logic

  business-logic -->network-logic
  
  business-logic -->storage-logic

  resources-logic -->storage-logic

  resources-logic -->business-logic

  resources-logic --> authentication-logic
  business-logic --> authentication-logic
```


## License

### License details

Copyright (c) 2023 European Commission

Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
except in compliance with the Licence.

You may obtain a copy of the Licence at:
https://joinup.ec.europa.eu/software/page/eupl

Unless required by applicable law or agreed to in writing, software distributed under 
the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF 
ANY KIND, either express or implied. See the Licence for the specific language 
governing permissions and limitations under the Licence.
