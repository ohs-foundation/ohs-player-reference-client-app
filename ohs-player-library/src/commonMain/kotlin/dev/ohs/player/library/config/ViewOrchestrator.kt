package dev.ohs.player.library.config

import dev.ohs.fhir.model.r4.Resource
import dev.ohs.player.library.model.SearchResult
import dev.ohs.player.library.tranformer.DataTransformer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlinx.serialization.descriptors.StructureKind

/**
 * The ViewOrchestrator assembles multiple transformed resources into one.
 */
class ViewOrchestrator(val transformer: DataTransformer) {

    /**
     * Assembles a composite state based on its SerialDescriptor.
     * @param result The SearchResult containing primary and included resources.
     * @param viewDefinitions A map of all loaded ViewDefinitions from the IG.
     */
    inline fun <reified T> assemble(
        result: SearchResult<out Resource>,
        viewDefinitions: Map<String, ViewDefinition>
    ): T {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

        val descriptor = serializer<T>().descriptor
        val context = result.allResources()

        val jsonObject = buildJsonObject {
            for (i in 0 until descriptor.elementsCount) {
                val slotKey = descriptor.getElementName(i)
                val elementDescriptor = descriptor.getElementDescriptor(i)
                
                val isList = elementDescriptor.kind == StructureKind.LIST
                val targetDescriptor = if (isList) elementDescriptor.getElementDescriptor(0) else elementDescriptor
                
                val targetStateName = targetDescriptor.serialName.substringAfterLast('.').removeSuffix("?")

                val viewDef = viewDefinitions[targetStateName]
                    ?: throw IllegalArgumentException("ViewDefinition for state $targetStateName not found")

                val targetType = viewDef.resource

                // 1. Check if the slot matches the Primary Resource
                if (result.resource::class.simpleName == targetType) {
                    val transformed = transformer.extractToJson(result.resource, viewDef, context)
                    if (isList) {
                        put(slotKey, JsonArray(listOf(transformed)))
                    } else {
                        put(slotKey, transformed)
                    }
                } 
                // 2. Otherwise, look in the 'Included' bucket
                else if (result.included?.containsKey(targetType) == true) {
                    val resources = result.included[targetType]!!
                    val transformedList = resources.map { transformer.extractToJson(it, viewDef, context) }
                    if (isList) {
                        put(slotKey, JsonArray(transformedList))
                    } else {
                        transformedList.firstOrNull()?.let { put(slotKey, it) }
                    }
                }
                // 3. Otherwise, look in the 'Reverse Included' bucket
                else if (result.revIncluded?.keys?.any { it == targetType } == true) {
                    val resources = result.revIncluded.filter { it.key == targetType }.values.flatten()
                    val transformedList = resources.map { transformer.extractToJson(it, viewDef, context) }
                    if (isList) {
                        put(slotKey, JsonArray(transformedList))
                    } else {
                        transformedList.firstOrNull()?.let { put(slotKey, it) }
                    }
                }
            }
        }
        return json.decodeFromJsonElement(jsonObject)
    }
}