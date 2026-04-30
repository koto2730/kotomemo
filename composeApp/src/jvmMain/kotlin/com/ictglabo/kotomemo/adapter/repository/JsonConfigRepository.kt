package com.ictglabo.kotomemo.adapter.repository

import com.ictglabo.kotomemo.entity.AppConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

class JsonConfigRepository(
    private val configFile: Path,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true },
) {
    fun load(): AppConfig {
        if (!Files.exists(configFile)) return AppConfig.EMPTY
        return runCatching {
            val text = Files.readString(configFile)
            if (text.isBlank()) AppConfig.EMPTY else json.decodeFromString<AppConfig>(text)
        }.getOrElse { AppConfig.EMPTY }
    }

    fun save(config: AppConfig) {
        val parent = configFile.parent
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
        }
        val text = json.encodeToString(config)
        Files.writeString(configFile, text)
    }
}
