package dev.ohs.player.library.transform

import dev.ohs.fhir.model.r4.Resource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

class FhirExtractionResult(
    val resource: Resource,
    val values: Map<String, String?>,
    @PublishedApi internal val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
) {
    operator fun get(field: String): String? = values[field]

    fun toJsonObject(): JsonObject = JsonObject(
        values.mapValues { (_, v) ->
            if (v != null) JsonPrimitive(v) else JsonNull
        }
    )

    inline fun <reified T : Any> decode(): T =
        json.decodeFromJsonElement(toJsonObject())
}