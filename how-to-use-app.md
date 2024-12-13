# How to use the application

## Minimum device requirements

- API level 26.

## Prerequisites

You can download the application [here](https://install.appcenter.ms/orgs/eu-digital-identity-wallet/apps/eudi-reference-android/distribution_groups/eudi%20wallet%20(demo)%20public)

You will also need to download the Android Verifier app. More information can be found [here](wiki/verifier_proximity.md)

## App launch

1. Launch the application
2. You will be presented with a welcome screen where you will be asked to create a PIN for future logins.

## Issuance flow (Scoped)

1. Open the "Add document" screen or if it's the first time you open the app, you will be redirected there after you enter or set up your PIN.
2. Pick "National ID".
3. From the web view that appears select the "FormEU" option and tap submit.
4. Fill in the form. Any data will do.
5. You will be shown a success screen. Tap next.
6. Your "National ID" is displayed. Tap "Continue".
7. You are now on the "Dashboard" screen.

## Issuance flow (Credential Offer)

1. Open the "Add document" screen or if it's the first time you open the app, you will be redirected there after you enter or set up your PIN.
2. Tap "SCAN QR".
3. Scan The QR Code from the issuer's website [EUDI Issuer](https://issuer.eudiw.dev/credential_offer_choice)
4. Review the documents contained in the credential offer and tap "Issue".
5. You will be shown a success screen. Tap "Continue".
6. You are now on the "Dashboard" screen.

While on the "Dashboard" screen you can tap "Add doc" and issue a new document, e.g. "Driving License".

If you want to re-issue a document you must delete it first by tapping on the document in the "Dashboard" screen and tapping the delete icon in the "Document details" view.

## Presentation (Online authentication/Same device) flow.

1. Go to the browser application on your device and enter "https://verifier.eudiw.dev"
2. Tap the first option (selectable) and pick the fields you want to share (e.g. "Family Name" and "Given Name")
3. Tap "Next" and then "Authorize".
4. When asked to open the wallet app tap "Open".
5. You will be returned to the app to the "Request" screen. Tap "Share".
6. Enter the PIN you added in the initial steps.
7. On success tap "Continue".
8. A browser will open showing that the Verifier has accepted your request.
9. Return to the app. You are back to the "Dashboard" screen and the flow is complete.

## Proximity flow

1. The user logs in successfully to the EUDI Wallet app and views the dashboard.
2. The user clicks the 'SHOW QR/TAP' button to display the QR code.
3. The Relying Party scans the presented QR code.
4. EUDI Wallet User can view the requested data set from the relying party.

    1. The distinction between mandatory and optional data elements is depicted.
    2. The requestor (i.e. relying party) of the data is depicted.
    3. EUDI Wallet User may select additional optional attributes to be shared.
5. EUDI Wallet User selects the option to share the attributes.
6. EUDI Wallet authenticates to share data (quick PIN).
7. User authorization is accepted - a corresponding message is displayed to the  EUDI Wallet User.
