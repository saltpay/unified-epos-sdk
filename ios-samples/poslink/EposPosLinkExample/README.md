# iOS PosLink Sample App

A sample iOS app demonstrating integration with the Teya Unified ePOS SDK using **PosLink**. The app implements a simple point-of-sale flow: browsing a product catalog, adding items to a basket, processing a payment with optional tip, and printing a receipt.

Documentation for the SDK using PosLink on iOS can be found [here](https://docs.teya.com/epos-sdk/poslink/ios/getting-started).

## Prerequisites

- Xcode
- iOS 16.0+
- A Teya terminal to which you can link the app
- Client credentials (`clientId` and `clientSecret`) from [partner.teya.xyz](https://partner.teya.xyz) (development environment)

## Getting Started

1. Clone the repository and open the `ios-samples/poslink/EposPosLinkExample` project in Xcode.

2. Add your credentials in `TeyaService.swift`:
   ```swift
   let teyaPosLinkSDK = TeyaPosLinkSDKKt.initialize(
       authConfig: PosLinkSDKAuthConfigManaged(
           clientId: "YOUR_CLIENT_ID",
           clientSecret: "YOUR_CLIENT_SECRET"
       ),
       isProductionEnv: false,
       // ...
   )
   ```

3. Build and run the app on an iOS device.

## Tech Stack

- **Language:** Swift
- **UI:** SwiftUI
- **Architecture:** @Observable ViewModel pattern
