# How to configure the application

## General configuration

The application allows the configuration of:

1. Wallet Host Url
2. Verifier API

Via the *EudiWalletConfig* class.

1. Trusted certificates

Via the *ProximityConfig* struct.

The applications certificates are located here:

https://github.com/niscy-eudiw/eudi-app-android-wallet-ui/tree/main/resources-logic/src/main/res/raw

You will also find the IACA certificate here. (trusted iaca root certificates).

ProximityConfig
## Theme configuration

The application allows the configuration of:

1. Colors
2. Images
3. Shape
4. Fonts
5. Dimension

Via *ThemeManager.Builder()*.