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

// Phase 5c: probe Native Image directly. The official org.graalvm.buildtools
// Gradle plugin does not register tasks under a kotlin("multiplatform")
// project, so we drive native-image ourselves. This first cut is intentionally
// minimal: no reachability metadata, no agent traces, no module tweaks. The
// goal is to see exactly where compilation explodes.
val nativeImageOutputDir = layout.buildDirectory.dir("native/nativeCompile")
val nativeImageExe = graal21Launcher.get().metadata.installationPath.asFile
    .resolve("bin/native-image.cmd")

tasks.register<Exec>("nativeCompile") {
    group = "build"
    description = "Build a Native Image executable with GraalVM"
    notCompatibleWithConfigurationCache(
        "Custom native-image task resolves the runtime classpath at execution time."
    )
    dependsOn(tasks.named("jvmJar"))

    val outputDir = nativeImageOutputDir.get().asFile
    workingDir = outputDir

    val jvmJarTask = tasks.named<Jar>("jvmJar")
    val runtimeClasspath = configurations.named("jvmRuntimeClasspath")

    inputs.files(jvmJarTask)
    inputs.files(runtimeClasspath)
    outputs.file(outputDir.resolve("kotomemo.exe"))

    doFirst {
        outputDir.mkdirs()
        // Forward slashes avoid backslash-escape headaches in the @argfile.
        val cp = (runtimeClasspath.get().files + jvmJarTask.get().archiveFile.get().asFile)
            .joinToString(File.pathSeparator) { it.absolutePath.replace("\\", "/") }

        // Windows command line is capped at ~32 KB; Compose's classpath blows
        // past that easily. Pass arguments via a Java @argfile instead.
        val argFile = outputDir.resolve("native-image.args").apply {
            writeText(
                buildString {
                    appendLine("-cp")
                    appendLine(cp)
                    appendLine("--no-fallback")
                    appendLine("-H:+UnlockExperimentalVMOptions")
                    appendLine("-H:Name=kotomemo")
                    appendLine("com.ictglabo.kotomemo.framework.MainKt")
                },
            )
        }
        commandLine(nativeImageExe.absolutePath, "@${argFile.absolutePath}")
    }
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
                iconFile.set(rootProject.file("assets/icon.ico"))
            }
            macOS {
                bundleID = "com.ictglabo.kotomemo"
                // TODO: provide assets/icon.icns once a Mac is available to run
                //   iconutil -c icns assets/icon.iconset
                // (jpackage requires .icns on macOS; .png/.ico are not accepted.)
            }
            linux {
                debMaintainer = "ictglabo"
                menuGroup = "Utilities"
                iconFile.set(rootProject.file("assets/icon.png"))
            }
        }
    }
}
