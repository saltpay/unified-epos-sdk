# JVM Windows PosLink Payment Sample — Design

## Goal
A minimal JVM desktop app that runs on Windows and takes a single card payment by calling
the Teya Unified ePOS PosLink SDK **directly in-process** — no JSON-RPC, no bundled
`TeyaEposIntegrationsApp.exe` wrapper.

## Integration model
Depends directly on the published Maven artifact:

```
com.teya.epos:unified-sdk-poslink-jvm-<platform>:1.8.0
```

resolved from Maven Central + `google()` (the latter for androidx transitive deps). `<platform>`
is auto-detected (`windows-x64`, `macos-arm64`, `linux-x64`, …) so the sample builds and smoke-tests
on a developer Mac and runs on the Windows target. A real integrator hardcodes their platform.

The SDK renders its own login and device-linking window (Compose, via a bundled skiko native lib),
so the host app never touches Compose. The physical Teya terminal is the payment interaction surface.

## Flow
1. **Initialise** — `TeyaPosLinkSDK.init(AuthConfig.Managed(clientId, clientSecret), isProductionEnv=false, eposInstanceId, logger)` → `PosLinkSDK`.
2. **Setup** — `sdk.setup(onFailure, onSuccess)`; SDK opens its own window for operator login + terminal linking.
3. **Make payment** — `sdk.getTransactionsApi().makePayment(uuid, amountMinor, "GBP", tipOrNull, null, null)` → `PaymentStateSubscription`; `.subscribe(listener)` logs each state and stops on `state.isFinal()`.

## UI (single Swing `JFrame`)
- Fields: Client ID, Client secret, ePOS instance ID, Amount (minor units, e.g. `1000` = £10.00).
- Buttons enabled in sequence: **1. Initialise → 2. Setup → 3. Make payment**.
- Scrolling SDK log pane.

## Project layout
```
windows-samples/poslink-jvm/EposPosLinkJvmExample/
  build.gradle.kts        application plugin, platform detection, SDK dependency
  settings.gradle.kts     mavenCentral + google
  gradlew, gradlew.bat, gradle/wrapper/…   (Gradle 8.14.2 wrapper)
  src/main/java/com/teya/epos/samples/poslink/
      PosLinkJvmSampleApp.java   UI + init/setup/pay
      SmokeTest.java             headless classpath check (no terminal needed)
  README.md
```

Credentials are typed into the fields at runtime; nothing secret is committed.

## Verification
- **Mac (no terminal):** `./gradlew smokeTest` (classpath resolves, skiko loads, `init` runs from Java) and `./gradlew compileJava`.
- **Windows (with terminal + creds):** `gradlew.bat run` → Initialise → Setup → enter amount → Make payment → tap card.

## Scope (YAGNI)
Sale payment only. No tip UI (tip passed as `null`), no receipt printing, no Pay-at-Table, no basket.
