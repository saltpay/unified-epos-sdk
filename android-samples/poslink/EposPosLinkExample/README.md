# Android PosLink Sample App

A sample Android app demonstrating integration with the Teya Unified ePOS SDK using **PosLink**. The app has two flows, switchable via the bottom navigation bar:

- **Sale** — a simple point-of-sale flow: browse a product catalog, add items to a basket, take a payment with optional tip, and print a receipt.
- **Pay at Table** — an event-driven integration where terminals drive the bill and payment for open tabs (tables), and the app responds.

Documentation for the SDK using PosLink on Android can be found [here](https://docs.teya.com/epos-sdk/poslink/android/getting-started).

## Prerequisites

- Android Studio
- Min SDK 24 (Android 7.0)
- A Teya terminal to which you can link the app
- Client credentials (`clientId` and `clientSecret`) from [partner.teya.xyz](https://partner.teya.xyz) (development environment)

## Getting Started

1. Clone the repository and open the `android-samples/poslink/EposPosLinkExample` project in Android Studio.

2. Add your credentials in `TeyaUtils.kt` by setting `clientId` and `clientSecret` in the `AuthConfig.Managed` block.
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
   > **Important:** Do not commit your credentials to version control. Keep them in a local, untracked file (e.g. `local.properties` or environment variables) and reference them from your build configuration.

3. Build and run the app on an Android device.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Teya's **Lemonade** design system (`com.teya.foundation:lemonade-ui`)
- **Navigation:** Navigation Compose (bottom navigation bar)
- **Architecture:** ViewModel with Compose state
