package com.ictglabo.kotomemo.entity

import java.util.UUID

@JvmInline
value class ContentsId(val value: String) {
    companion object {
        fun generate(): ContentsId = ContentsId(UUID.randomUUID().toString())
    }
}
