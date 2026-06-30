# Windows PosLink Sample App

A sample Windows app demonstrating integration with the Teya Unified ePOS SDK using **PosLink**. The app has two flows, switchable via the left navigation pane:

- **Sale** — a simple point-of-sale flow: browse a product catalog, add items to a basket, take a payment with optional tip, and print a receipt.
- **Pay at Table** — an event-driven integration where terminals drive the bill and payment for open tabs (tables), and the app responds.

Documentation for the SDK using PosLink on Windows can be found [here](https://docs.teya.com/epos-sdk/poslink/windows/getting-started).

## Prerequisites

- Visual Studio 2022 (with the .NET desktop development and Windows App SDK workloads)
- .NET 8.0 SDK
- Windows 10 version 1809 (build 17763) or later
- A Teya terminal to which you can link the app
- Client credentials (`clientId` and `clientSecret`) from [partner.teya.xyz](https://partner.teya.xyz) (development environment)

## Getting Started

1. Clone the repository and open the `windows-samples/poslink/EposPosLinkExample` solution in Visual Studio.

2. Add your credentials in `TeyaSdkManager.cs` by setting `clientId` and `clientSecret` in the `Initialize` method.
    ```csharp
    var parameters = new
    {
        requesterId = "epos-app-id",
        requesterVersion = "1.0.0",
        isProductionEnv = false,
        clientId = "YOUR_CLIENT_ID",
        clientSecret = "YOUR_CLIENT_SECRET"
    };
    ```
   > **Important:** Do not commit your credentials to version control. Keep them in a local, untracked file (e.g. environment variables or a user-secrets store) and reference them from your build configuration.

3. Build and run the app on a Windows device.

## Tech Stack

- **Language:** C#
- **UI:** WinUI 3 (Windows App SDK) with XAML
- **Navigation:** `NavigationView` (left pane)
- **Architecture:** MVVM with CommunityToolkit.Mvvm
