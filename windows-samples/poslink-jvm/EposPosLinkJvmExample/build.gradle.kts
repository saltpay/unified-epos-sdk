plugins {
    application
}

val teyaSdkVersion = "1.8.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// The platform this app ships to. A real ePOS app knows its answer and writes it literally:
//
//     val teyaPlatform = "windows-x64"
//
// The sample detects it instead, only so it runs on whatever machine you happen to be on.
val teyaPlatform: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = when (System.getProperty("os.arch").lowercase()) {
        "aarch64", "arm64" -> "arm64"
        else -> "x64"
    }
    when {
        os.contains("mac") -> "macos-$arch"
        os.contains("windows") -> "windows-$arch"
        else -> "linux-$arch"
    }
}

dependencies {
    // Naming the platform in the coordinate is the whole setup. The SDK draws its own login and
    // device-linking screens, which render through a native library — this artifact pairs the SDK
    // with the right one, at a version we keep in step, so it never has to appear here.
    implementation("com.teya.epos:unified-sdk-poslink-jvm-$teyaPlatform:$teyaSdkVersion")
}

application {
    mainClass = "com.teya.epos.samples.poslink.PosLinkJvmSampleApp"
}

tasks.register<JavaExec>("smokeTest") {
    description = "Headless check that the resolved classpath can actually run the SDK."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.teya.epos.samples.poslink.SmokeTest"
}
