package dev.ohs.player.library.domain.util

import kotlinx.serialization.json.Json

object JsonUtil {
    val json: Json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
}
