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


// Resolve the GraalVM 21 toolchain so it can be used independently of JAVA_HOME.
val graal21Launcher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
    vendor.set(JvmVendorSpec.GRAAL_VM)
}

compose.desktop {
    application {
        mainClass = "com.ictglabo.kotomemo.framework.MainKt"
        javaHome = graal21Launcher.get().metadata.installationPath.asFile.absolutePath

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "kotomemo"
            packageVersion = "1.0.0"
            description = "kotomemo - a Notepad-style editor with markdown and AI integration."
            vendor = "ictglabo"
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
