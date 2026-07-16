# Windows PosLink JVM Sample App

A minimal **JVM desktop** app (plain Java + Swing) that takes a single card payment using the Teya
Unified ePOS SDK via **PosLink**, calling the SDK **directly in-process** — no JSON-RPC bridge and
no bundled helper executable.

The app is deliberately tiny: initialise the SDK, run setup (log in and link a terminal), then make
one payment. The SDK draws its own login, device-linking, and payment windows, so this sample never
touches Compose — it just calls `init()`, `setup()`, and `makePayment()`. The physical Teya terminal
is where the card is tapped.

## How the SDK is integrated

The whole setup is one Maven dependency, in `build.gradle.kts`:

```kotlin
implementation("com.teya.epos:unified-sdk-poslink-jvm-$teyaPlatform:1.8.0")
```

Naming the platform in the coordinate pairs the SDK with the right native rendering library. This
sample **detects** the platform (`windows-x64`, `macos-arm64`, …) so it runs on whatever machine you
build on. A real ePOS app knows its target and hardcodes it, e.g.:

```kotlin
val teyaPlatform = "windows-x64"
```

Artifacts resolve from Maven Central plus `google()` (for androidx transitive dependencies).

## Prerequisites

- JDK 17 or later
- Windows 10 or later to run against a terminal (builds fine on macOS/Linux too)
- A Teya terminal you can link the app to
- Client credentials (`clientId` and `clientSecret`) from
  [partner.teya.xyz](https://partner.teya.xyz) (development environment)

## Run it

From this directory:

```bat
gradlew.bat run
```

(on macOS/Linux: `./gradlew run`)

Then in the window:

1. Enter your **Client ID** and **Client secret**.
2. **1. Initialise SDK**.
3. **2. Setup** — the SDK opens its own window; log in and link your terminal.
4. Enter an **Amount** in minor units (e.g. `1000` = £10.00) and press **3. Make payment**.
5. Follow the prompts on the terminal to tap the card. Payment states are printed in the log pane.

Credentials are typed in at runtime and never written to disk by this app. Do not commit them.

## Smoke test (no terminal needed)

Checks that the classpath resolves and the SDK can be initialised from plain Java:

```bat
gradlew.bat smokeTest
```

## Tech stack

- **Language:** Java 17
- **UI:** Swing (single `JFrame`)
- **Build:** Gradle (`application` plugin, wrapper included)
- **SDK:** `com.teya.epos:unified-sdk-poslink-jvm-<platform>:1.8.0`
