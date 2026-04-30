import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serializationJson)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}


// Resolve the GraalVM 21 toolchain so the Kotlin compiler / tests run on it
// regardless of JAVA_HOME. Phase 5c (Native Image) will lean on this.
val graal21Launcher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
    vendor.set(JvmVendorSpec.GRAAL_VM)
}

// Use a vanilla Oracle JDK 21 for the packaged application runtime. GraalVM CE
// 21's jlink'd runtime fails to start when bundled by jpackage ("JVM not
// found") — its native JIT (jvmcicompiler.dll) needs modules that the trimmed
// runtime image does not carry. Disabling JVMCI was not enough, so the bundled
// runtime needs to be a plain HotSpot JDK. Compile-time tasks still run on
// GraalVM via kotlin.jvmToolchain above; only the runtime that ships with the
// installer changes here.
val packagingLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
    vendor.set(JvmVendorSpec.ORACLE)
}

compose.desktop {
    application {
        mainClass = "com.ictglabo.kotomemo.framework.MainKt"
        javaHome = packagingLauncher.get().metadata.installationPath.asFile.absolutePath

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "kotomemo"
            packageVersion = "1.0.0"
            description = "kotomemo - a Notepad-style editor."
            vendor = "ictglabo"

            // Modules that Compose / Kotlin commonly need but jpackage's
            // module discovery can miss when only the project's own classes
            // are scanned. Listing them explicitly avoids a class of cryptic
            // launch failures.
            //
            // java.net.http is required by adapter.http.JdkHttpClient (Phase 4).
            // The auto-discovery somehow misses it, so the bundled runtime ends
            // up without HttpClient and the app crashes at startup with
            // NoClassDefFoundError: java/net/http/HttpClient.
            modules(
                "java.net.http",
                "jdk.unsupported",
                "java.naming",
                "java.management",
                "java.sql",
                "java.scripting",
                "java.security.jgss",
                "jdk.crypto.cryptoki",
            )
            // TODO: re-enable once we have an RTF license file (WiX light.exe rejects plain text).
            // licenseFile.set(rootProject.file("LICENSE"))

            windows {
                menu = true
                shortcut = true
                // Stable upgrade UUID - keep this fixed across versions or upgrades
                // will install side-by-side instead of replacing the previous one.
                upgradeUuid = "8c1b2b34-7a45-4d22-9b3e-f0c8b7a6d5e1"
            }
            macOS {
                bundleID = "com.ictglabo.kotomemo"
            }
            linux {
                debMaintainer = "ictglabo"
                menuGroup = "Utilities"
            }
        }
    }
}
