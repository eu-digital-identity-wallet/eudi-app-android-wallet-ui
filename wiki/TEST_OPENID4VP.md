# How to test OPENID4VP

1. In the [build.gradle](../app/build.gradle) file of EUDI Wallet UI app, set the verifier API and the
   host name of your OpenId4Vp verifier, as following:

   ```groovy
    // ...
    android {
        // ...
        defaultConfig {
            // ...
            buildConfigField "String", "VERIFIER_API", "\"https://example-openid4vp-verifier.com\""
            manifestPlaceholders.verifierHostName = "example-openid4vp-verifier"
            // ...
      }
      // ...
   }
   // ...
   ```
2. Build and run the EUDI Wallet UI app on a mobile device.
3. On your mobile, go to **Settings -> Apps-> EUDIW -> Open by default**, click on **Add Link** and
   add the verifier link to allow it to open in the EUDIW app.

   **Notice:** on Android 12 and higher, users must verify the App links manually.

## Same Device Scenario

1. Open the verifier website in the browser of your mobile device (the same device that the EUDIW
   UI app is installed on) and click on the button or link that prompts you to authenticate.
2. The EUDI Wallet UI app opens and prompts you to select the data fields you want to share.
3. Click on the **Send** button to send your response.
4. After the response has been transferred successfully, the EUDI Wallet UI app closes and returns to
   the verifier web site.

## Cross-Device Scenario

1. Open the verifier web site in the browser of your PC (or in another device).
2. Scan the QR code provided in the verifier web site using an app that supports QR code scanning
   and tap on the link. For example, you can use the Camera app on your mobile device and tap on
   the pop-up message that appears.
3. The EUDI Wallet app opens and prompts you to select the data fields you want to share.
4. After the response has been transferred successfully, the EUDI Wallet UI app closes.