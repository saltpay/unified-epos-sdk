# Java PosLink Sample App

A sample Java desktop app demonstrating integration with the Teya Unified ePOS SDK using **PosLink**. The app has two flows, switchable via the left navigation pane:

- **Sale** — a simple point-of-sale flow: browse a product catalog, add items to a basket, take a payment with optional tip, and print a receipt.
- **Pay at Table** — an event-driven integration where terminals drive the bill and payment for open tabs (tables), and the app responds.

Documentation for the SDK using PosLink can be found [here](https://docs.teya.com/epos-sdk/poslink/introduction/getting-started).

## Prerequisites

- JDK 17 or later
- A Teya terminal to which you can link the app
- Client credentials (`clientId` and `clientSecret`) from [partner.teya.xyz](https://partner.teya.xyz) (development environment)

## Getting Started

1. Clone the repository and open the `java-samples/poslink/EposPosLinkExample` project in your IDE.

2. Set the SDK artifact id in `pom.xml` to match your platform (`macos-arm64`, `macos-x64`, `windows-x64`, `windows-arm64`, `linux-x64` or `linux-arm64`). The sample ships with `windows-x64`.
    ```xml
    <dependency>
        <groupId>com.teya.epos</groupId>
        <artifactId>unified-sdk-poslink-jvm-windows-x64</artifactId>
        <version>${teya.sdk.version}</version>
        <type>pom</type>
    </dependency>
    ```

3. Add your credentials in `TeyaSdkManager.java` by setting `CLIENT_ID` and `CLIENT_SECRET`, which are passed to `PosLinkSDK.AuthConfig.Managed`.
    ```java
    teyaPosLinkSDK = TeyaPosLinkSDK.init(
            new PosLinkSDK.AuthConfig.Managed(CLIENT_ID, CLIENT_SECRET),
            false,
            null,
            new LoggerImpl());
    ```
   > **Important:** Do not commit your credentials to version control. Keep them in a local, untracked file (e.g. environment variables or a properties file) and reference them from your configuration.

4. Build and run the app.
    ```shell
    # macOS / Linux
    ./mvnw javafx:run

    # Windows
    mvnw.cmd javafx:run
    ```

## Tech Stack

- **Language:** Java 17
- **UI:** JavaFX 17 with FXML
- **Build:** Maven
