# Android PosLink Sample App

A sample Android app demonstrating integration with the Teya Unified ePOS SDK using **PosLink**. The app has two flows, switchable via the bottom navigation bar:

- **Order** — a simple point-of-sale flow: browse a product catalog, add items to a basket, take a payment with optional tip, and print a receipt.
- **Pay at Table** — an event-driven integration where terminals drive the bill and payment for open tabs (tables/orders), and the app responds.

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

## Pay at Table

The **Pay at Table** tab demonstrates the SDK's Tabs / Pay at Table (PAT) integration.

Unlike the Order screen — where the app initiates the payment — PAT is **event-driven** and the ePOS app is a *responder*. The terminal drives the customer interaction; the app:

1. enables PAT on the store (`tabsApi.setPayAtTableEnabledOnStore`),
2. opens a tab from a basket (`tabsApi.openTab`),
3. subscribes to the terminal event stream (`tabsApi.tabEvents.subscribe`),
4. responds to terminal-initiated events — `onShowBillRequested` → `respondToBillRequest`, `onPayRequested` → `transactionsApi.makePayment(tabContext = ...)`,
5. closes the tab when fully paid (`tabsApi.closeTab`).

The on-screen event log shows each callback as it arrives.

### Building against the SDK with Pay at Table

Pay at Table ships in Unified ePOS SDK **1.6.0**. Until that version is published, this sample resolves a locally-built SDK from your Maven Local repository:

1. In the SDK repository, publish to Maven Local:
   ```bash
   SDK_GROUP_ID=com.teya.epos SDK_VERSION=1.6.0-SNAPSHOT \
     ./gradlew :unified-epos-sdk:poslink:umbrella:publishWithDependenciesToMavenLocal
   ```
2. This sample already has `mavenLocal()` in `settings.gradle.kts` and depends on `com.teya.epos:unified-sdk-poslink:1.6.0-SNAPSHOT` in `app/build.gradle.kts`.

> **Note:** this Maven Local setup is temporary. Once 1.6.0 is published, switch the dependency in `app/build.gradle.kts` to the released coordinates and you can remove `mavenLocal()`.

Full end-to-end testing requires a Teya terminal in Pay at Table mode and valid store credentials.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material Design 3
- **Architecture:** ViewModel with Compose state
