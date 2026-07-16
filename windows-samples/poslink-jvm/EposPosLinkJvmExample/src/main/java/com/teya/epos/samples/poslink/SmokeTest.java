package com.teya.epos.samples.poslink;

import com.teya.sdkutilities.NoOpLogger;
import com.teya.unifiedepossdk.TeyaPosLinkSDK;
import com.teya.unifiedepossdk.poslink.PosLinkSDK;

/**
 * Headless check that the resolved classpath is actually complete: the skiko native library
 * loads, Dispatchers.Main is backed by the Swing dispatcher, Ktor finds an engine, and the SDK
 * entry point can be called from plain Java. Needs no terminal and no credentials. Run with:
 *
 *   ./gradlew smokeTest
 */
public final class SmokeTest {

    public static void main(String[] args) {
        check("skiko native library", () -> {
            Class<?> library = Class.forName("org.jetbrains.skiko.Library");
            Object instance = library.getField("INSTANCE").get(null);
            library.getMethod("load").invoke(instance);
            return "loaded";
        });

        check("Dispatchers.Main (needs kotlinx-coroutines-swing)", () -> {
            Class<?> dispatchers = Class.forName("kotlinx.coroutines.Dispatchers");
            Object main = dispatchers.getMethod("getMain").invoke(null);
            return main.toString();
        });

        check("Ktor engine discovery", () -> {
            Class<?> factory = Class.forName("io.ktor.client.HttpClientEngineContainer");
            java.util.List<String> found = new java.util.ArrayList<>();
            java.util.ServiceLoader.load(factory).forEach(e -> found.add(e.toString()));
            return found.isEmpty() ? "NONE FOUND" : String.join(", ", found);
        });

        check("TeyaPosLinkSDK.init from Java (prepares crypto internally)", () -> {
            PosLinkSDK sdk = TeyaPosLinkSDK.init(
                    new PosLinkSDK.AuthConfig.Managed("dummy-client-id", "dummy-client-secret"),
                    false,
                    "smoke-test",
                    NoOpLogger.INSTANCE
            );
            return sdk.getClass().getName();
        });
    }

    private interface Check {
        String run() throws Exception;
    }

    private static void check(String name, Check check) {
        try {
            System.out.printf("  OK    %-48s %s%n", name, check.run());
        } catch (Throwable t) {
            Throwable cause = t.getCause() != null ? t.getCause() : t;
            System.out.printf("  FAIL  %-48s %s: %s%n", name, cause.getClass().getSimpleName(), cause.getMessage());
        }
    }
}
