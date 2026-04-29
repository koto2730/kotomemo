package com.ictglabo.kotomemo.framework.config

import java.nio.file.Files
import java.nio.file.Path

object ConfigPaths {
    private const val DIR_NAME = ".kotomemo"
    private const val CONFIG_FILE = "config"

    val configDir: Path
        get() = Path.of(System.getProperty("user.home"), DIR_NAME)

    val configFile: Path
        get() = configDir.resolve(CONFIG_FILE)

    fun ensureDir() {
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir)
        }
    }
}
