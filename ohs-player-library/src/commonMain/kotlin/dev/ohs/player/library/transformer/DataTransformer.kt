package dev.ohs.player.library.transformer

import dev.ohs.fhir.fhirpath.FhirPathEngine
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.player.library.model.ViewDefinition
import kotlinx.serialization.json.Json


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
    override fun extract(
        resource: Resource,
        viewDefinition: ViewDefinition
    ): FhirExtractionResult {

        val values = viewDefinition.allColumns()
            .filter { it.name != null && it.path != null }
            .associate { column ->
                column.name!! to evaluatePath(resource, column.path!!)
            }
        return FhirExtractionResult(
            resource = resource,
            values = values,
            json = jsonConfiguration)

    }

    inline fun <reified T: Any> transform(resource: Resource, viewDefinition: ViewDefinition) : T =
        extract(resource, viewDefinition).decode()

    private fun evaluatePath(focus: Any?, path: String): String? {
        return try {
            val results : Collection<Any> = fhirPathEngine.evaluateExpression(
                expression = path,
                base = focus,
                variables = emptyMap()
            )
            results.firstOrNull()?.toString()
        }catch (e: Exception){
            null
        }
    }

    companion object{
        fun forR4(): DataTransformer = DataTransformer(FhirPathEngine.forR4())
    }

}