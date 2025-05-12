# mDoc Verifier (Testing)

The mDoc Verifier (Testing) is an Android application based on the `appverifier` from the [Google Identity Credential library](https://github.com/openwallet-foundation-labs/identity-credential), implementing ISO/IEC 18013-5:2021.

## Purpose

The mDoc Verifier (Testing) app is provided to help developers test and validate their wallet implementations.

## Modifications

Starting from the original code of the `appverifier` [here](https://github.com/openwallet-foundation-labs/identity-credential/commit/0b9b31ef63047762e10300e23a22f6d7dcfb6d15), the following modifications have been made:

 - Support for requesting EU Documents:
   - Personal Identification Data (PID) document, according to the ARF PID RuleBook.
   - Age Verification (Pseudonym) document.
 - IACA Certificates: Updated to support EUDI Wallet IACAs as trusted certificates.
 - Reader Authentication Certificate.

The app is available for download through GitHub Releases, [here](https://github.com/eu-digital-identity-wallet/eudi-app-android-wallet-ui/releases) 
(the code is not currently publicly available).

## Important Note
The mDoc Verifier (Testing) app is a testing tool for developers to validate their wallet implementations. 
It is not intended for production use. 
The app may contain bugs or other issues that affect its functionality on different mobile devices or Android versions.
These issues will be addressed in the upcoming open-source library, which will be available for developers to build their own verifier applications.