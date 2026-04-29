package com.ictglabo.kotomemo.usecase.port

interface ConfigRepository {
    fun read(key: String): String?
    fun write(key: String, value: String)
}
