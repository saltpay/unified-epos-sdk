# Android PosLink Sample App

A sample Android app demonstrating integration with the Teya Unified ePOS SDK using **PosLink**. The app implements a simple point-of-sale flow: browsing a product catalog, adding items to a basket, processing a payment with optional tip, and printing a receipt.

Documentation for the SDK using PosLink on Android can be found [here](https://docs.teya.com/epos-sdk/poslink/android/getting-started).

## Prerequisites

- Android Studio
- Min SDK 24 (Android 7.0)
- A Teya terminal to which you can link the app
- Client credentials (`clientId` and `clientSecret`) from [partner.teya.xyz](https://partner.teya.xyz) (development environment)

## Getting Started

1. Clone the repository and open the `android-samples/poslink/EposPosLinkExample` project in Android Studio.

2. Add your credentials in `TeyaUtils.kt`:
   ```kotlin
   val teyaPosLinkSDK = TeyaPosLinkSDK(
       isProductionEnv = false,
       authConfig = PosLinkSDK.AuthConfig.Managed(
           clientId = "YOUR_CLIENT_ID",
           clientSecret = "YOUR_CLIENT_SECRET"
       ),
       // ...
   )
   ```

3. Build and run the app on an Android device.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material Design 3
- **Architecture:** ViewModel with Compose state
