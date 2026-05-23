import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.FileOutputStream
import java.net.URI

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

// ---------------------------------------------------------------------------
// Portable single-file exe for Windows.
//
// jpackage already produces an MSI installer and an app-image folder, but the
// folder is ~125 MB of files that users have to keep together. warp-packer
// wraps that folder into a single ~60 MB .exe that self-extracts to
// %LOCALAPPDATA%\warp\packages\<hash>\ on first launch and reuses the cache
// on subsequent launches. End-user UX: one file, double-click, no installer.
//
// After warp-packer runs we do two post-processing steps:
//   1. Patch the PE subsystem byte from Console (0x03) to Windows GUI (0x02)
//      so launching the .exe does not pop up a cmd window in the background.
//   2. Embed assets/icon.ico into the wrapper .exe via rcedit so File Explorer
//      shows the kotomemo icon for kotomemo-portable.exe.
//
// warp-packer 0.3.0 (2019) and rcedit 2.0.0 (Electron project) are pinned and
// downloaded into build/tools/ so local dev and CI use the same toolchain.
// ---------------------------------------------------------------------------

val warpPackerVersion = "0.3.0"
val warpPackerRelativePath = "tools/warp-packer-$warpPackerVersion.exe"
val rceditVersion = "2.0.0"
val rceditRelativePath = "tools/rcedit-$rceditVersion-x64.exe"

val downloadWarpPacker by tasks.registering {
    val targetFile = layout.buildDirectory.file(warpPackerRelativePath)
    val downloadUrl =
        "https://github.com/dgiagio/warp/releases/download/v$warpPackerVersion/windows-x64.warp-packer.exe"
    outputs.file(targetFile)
    doLast {
        val file = targetFile.get().asFile
        if (file.exists()) return@doLast
        file.parentFile.mkdirs()
        URI(downloadUrl).toURL().openStream().use { input: java.io.InputStream ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
    }
}

val downloadRcedit by tasks.registering {
    val targetFile = layout.buildDirectory.file(rceditRelativePath)
    val downloadUrl =
        "https://github.com/electron/rcedit/releases/download/v$rceditVersion/rcedit-x64.exe"
    outputs.file(targetFile)
    doLast {
        val file = targetFile.get().asFile
        if (file.exists()) return@doLast
        file.parentFile.mkdirs()
        URI(downloadUrl).toURL().openStream().use { input: java.io.InputStream ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
    }
}

val packagePortableExe by tasks.registering {
    group = "compose desktop"
    description = "Wraps the createDistributable app-image into a single portable .exe via warp-packer, " +
        "patches subsystem to GUI, and embeds the kotomemo icon."

    dependsOn("createDistributable", downloadWarpPacker, downloadRcedit)

    val appImageDir = layout.buildDirectory.dir("compose/binaries/main/app/kotomemo")
    val outputFile = layout.buildDirectory.file("portable/kotomemo-portable.exe")
    val warpPacker = layout.buildDirectory.file(warpPackerRelativePath)
    val rcedit = layout.buildDirectory.file(rceditRelativePath)
    val iconFile = rootProject.file("assets/icon.ico")

    inputs.dir(appImageDir)
    inputs.file(warpPacker)
    inputs.file(rcedit)
    inputs.file(iconFile)
    outputs.file(outputFile)

    doLast {
        val out = outputFile.get().asFile
        out.parentFile.mkdirs()

        // 1. Run warp-packer to create the self-extracting wrapper:
        //    [PE stub] [gzip archive overlay]
        val warpExit = ProcessBuilder(
            warpPacker.get().asFile.absolutePath,
            "--arch", "windows-x64",
            "--input_dir", appImageDir.get().asFile.absolutePath,
            "--exec", "kotomemo.exe",
            "--output", out.absolutePath,
        ).inheritIO().start().waitFor()
        if (warpExit != 0) throw GradleException("warp-packer failed (exit $warpExit)")

        // 2. Split the file into [PE stub] and [overlay archive]. rcedit
        //    rewrites the PE and drops trailing overlay data, so we save
        //    the overlay aside, modify the PE, then re-append.
        //
        //    PE end-on-disk = max(section.PointerToRawData + section.SizeOfRawData)
        //    across all section headers.
        val originalBytes = out.readBytes()
        fun u16(off: Int) = (originalBytes[off].toInt() and 0xFF) or
            ((originalBytes[off + 1].toInt() and 0xFF) shl 8)
        fun i32(off: Int) = (originalBytes[off].toInt() and 0xFF) or
            ((originalBytes[off + 1].toInt() and 0xFF) shl 8) or
            ((originalBytes[off + 2].toInt() and 0xFF) shl 16) or
            ((originalBytes[off + 3].toInt() and 0xFF) shl 24)

        val peOffset = i32(0x3C)
        val numberOfSections = u16(peOffset + 6)
        val sizeOfOptionalHeader = u16(peOffset + 20)
        val firstSectionHeader = peOffset + 24 + sizeOfOptionalHeader

        var peEnd = 0
        for (i in 0 until numberOfSections) {
            val sec = firstSectionHeader + i * 40
            val sectionEnd = i32(sec + 20) + i32(sec + 16) // PointerToRawData + SizeOfRawData
            if (sectionEnd > peEnd) peEnd = sectionEnd
        }
        val overlay = originalBytes.copyOfRange(peEnd, originalBytes.size)
        logger.lifecycle("warp-packer output: PE ${peEnd / 1024} KB + overlay ${overlay.size / 1024 / 1024} MB")

        // Write only the PE portion back so rcedit operates on a clean PE.
        out.writeBytes(originalBytes.copyOfRange(0, peEnd))

        // 3. Embed the kotomemo icon into the PE.
        val rceditExit = ProcessBuilder(
            rcedit.get().asFile.absolutePath,
            out.absolutePath,
            "--set-icon", iconFile.absolutePath,
        ).inheritIO().start().waitFor()
        if (rceditExit != 0) throw GradleException("rcedit failed (exit $rceditExit)")

        // 4. Patch PE subsystem: Console (0x03) -> Windows GUI (0x02). Done
        //    after rcedit in case rcedit ever shifts the optional header.
        val peBytes = out.readBytes()
        val peOffset2 = (peBytes[0x3C].toInt() and 0xFF) or
            ((peBytes[0x3D].toInt() and 0xFF) shl 8) or
            ((peBytes[0x3E].toInt() and 0xFF) shl 16) or
            ((peBytes[0x3F].toInt() and 0xFF) shl 24)
        val subsystemOffset = peOffset2 + 24 + 0x44
        if (peBytes[subsystemOffset].toInt() and 0xFF == 0x03) {
            peBytes[subsystemOffset] = 0x02
            peBytes[subsystemOffset + 1] = 0x00
            out.writeBytes(peBytes)
        }

        // 5. Re-append the overlay archive. warp-packer's runtime locates the
        //    embedded gzip archive by scanning for the gzip magic bytes
        //    (0x1f 0x8b 0x08), so the new PE size after rcedit is fine.
        FileOutputStream(out, true).use { stream -> stream.write(overlay) }
        logger.lifecycle("final portable exe: ${out.length() / 1024 / 1024} MB")
    }
}
