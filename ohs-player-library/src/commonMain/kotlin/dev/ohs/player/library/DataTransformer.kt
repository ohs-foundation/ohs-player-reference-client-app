package dev.ohs.player.library

import dev.ohs.fhir.fhirpath.FhirPathEngine
import dev.ohs.fhir.model.r4.Resource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
/**
 * Transforms a FHIR resource into a generated Kotlin type T.
 * Uses the OHS Kotlin FHIR SDK's FhirPathEngine for evaluation.
 */
class DataTransformer(
    private val fhirPathEngine: FhirPathEngine,
    @PublishedApi
    internal val jsonConfiguration: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
) : FhirExtractorRepository {

    override fun extract(request: FhirExtractionRequest): FhirExtractionResult {
        val values = request.paths.mapValues { (_, path) ->
            evaluatePath(request.resource, path, request.context, request.strict)
        }
        return FhirExtractionResult(
            resource = request.resource,
            values = values,
            json = jsonConfiguration
        )
    }

    fun extractToJson(request: FhirExtractionRequest): JsonObject =
        extract(request).toJsonObject()

    inline fun <reified T : Any> transform(request: FhirExtractionRequest): T =
        extract(request).decode()

    private fun evaluatePath(
        focus: Any?,
        path: String,
        context: List<Resource>,
        strict: Boolean
    ): String? {
        return try {
            val variables = if (context.isNotEmpty()) mapOf("context" to context) else emptyMap()
            val results: Collection<Any> = fhirPathEngine.evaluateExpression(
                expression = path,
                base = focus,
                variables = variables
            )
            results.firstOrNull()?.toString()
        } catch (e: Exception) {
            if (strict) throw e else null
        }
    }

    companion object {
        fun forR4(): DataTransformer = DataTransformer(FhirPathEngine.forR4())
    }
}