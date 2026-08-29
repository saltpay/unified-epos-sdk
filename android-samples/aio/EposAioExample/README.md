# Android All-In-One Sample App

A sample Android app demonstrating integration with the Teya Unified ePOS SDK using **All-In-One (AIO)**. The app has two flows, switchable via the bottom navigation bar:

- **Sale** — a simple point-of-sale flow: browse a product catalog, add items to a basket, take a payment with optional tip, and print a receipt.
- **History** — the transactions taken in this session, with the option to refund a payment.

Documentation for the SDK using AIO on Android can be found [here](https://docs.teya.com/epos-sdk/all-in-one/android/getting-started).

## Prerequisites

- Android Studio
- Min SDK 24 (Android 7.0)
- A device/emulator with the Teya Payments App installed

## Getting Started

1. Clone the repository and open the `android-samples/aio/EposAioExample` project in Android Studio.

2. Build and run the app on a device/emulator with the Teya Payments App installed.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Teya's **Lemonade** design system (`com.teya.foundation:lemonade-ui`)
- **Navigation:** Navigation Compose (bottom navigation bar)
- **Architecture:** ViewModel with Compose state
