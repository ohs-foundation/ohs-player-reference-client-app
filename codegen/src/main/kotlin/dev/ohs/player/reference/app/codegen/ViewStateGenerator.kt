package dev.ohs.player.reference.app.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import dev.ohs.fhir.model.r4.StructureDefinition
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.Base64

/**
 * Generates all state data classes into ViewStates.kt:
 *
 *  - Individual states from Binary-*View.json (e.g. PatientHeaderState)
 *  - Composite states from Binary-*Dashboard*.json (e.g. IpsDashboardState)
 *
 * For composite states, field names and cardinality are read from
 * StructureDefinition-DashboardManifest.json. The Binary instance payload
 * is parsed as a raw JsonObject keyed by those same SD field names, so
 * no hardcoded data model is required here.
 */
object ViewStateGenerator {

    private val json = Json { ignoreUnknownKeys = true }

    fun generate(igOutputDir: File, fshResourceDir: File, outputPackage: String): FileSpec? {
        val viewFiles = igOutputDir.listFiles()
            ?.filter { it.name.startsWith("Binary-") && it.name.endsWith("View.json") }
            ?: return null

        // Build canonical URL → state class name index while generating individual states
        val canonicalToStateName = mutableMapOf<String, String>()
        val individualTypes = viewFiles.mapNotNull { file ->
            decodePayload<ViewDefinitionData>(file)?.also { vd ->
                vd.url?.let { canonicalToStateName[it] = vd.name }
            }?.let { buildIndividualState(it) }
        }

        if (individualTypes.isEmpty()) return null

        val fileSpec = FileSpec.builder(outputPackage, "ViewStates")
        individualTypes.forEach { fileSpec.addType(it) }

        // Composite states: Generated purely from Logical Models (Schema-driven)
        // Identifying composite states by Logical Models ending in "DashboardState"
        fshResourceDir.listFiles()
            ?.filter { it.name.startsWith("StructureDefinition-") && it.name.endsWith("DashboardState.json") }
            ?.forEach { file ->
                val sd = parseSd(file)
                val stateName = sd.name?.value ?: return@forEach
                val sdFields = sd.sdFields()
                if (sdFields.isNotEmpty()) {
                    buildCompositeStateFromSchema(stateName, sdFields, outputPackage)?.let { 
                        fileSpec.addType(it) 
                    }
                }
            }

        return fileSpec.build()
    }

    private fun buildIndividualState(vd: ViewDefinitionData): TypeSpec {
        val ctor = FunSpec.constructorBuilder()
        val builder = TypeSpec.classBuilder(vd.name)
            .addModifiers(KModifier.DATA)
            .addAnnotation(Serializable::class)

        vd.allColumns().forEach { col ->
            val type = STRING.copy(nullable = true)
            ctor.addParameter(ParameterSpec.builder(col.name, type).defaultValue("null").build())
            builder.addProperty(PropertySpec.builder(col.name, type).initializer(col.name).build())
        }

        return builder.primaryConstructor(ctor.build()).build()
    }

    private fun buildCompositeStateFromSchema(
        stateName: String,
        sdFields: List<SdField>,
        outputPackage: String
    ): TypeSpec? {
        val ctor = FunSpec.constructorBuilder()
        val builder = TypeSpec.classBuilder(stateName)
            .addModifiers(KModifier.DATA)
            .addAnnotation(Serializable::class)

        sdFields.forEach { field ->
            val targetStateName = field.shortDesc ?: return@forEach
            val itemType = ClassName(outputPackage, targetStateName)
            val propType = if (field.isList) LIST.parameterizedBy(itemType) else itemType.copy(nullable = true)
            val default = if (field.isList) "emptyList()" else "null"

            ctor.addParameter(ParameterSpec.builder(field.name, propType).defaultValue(default).build())
            builder.addProperty(PropertySpec.builder(field.name, propType).initializer(field.name).build())
        }

        return builder.primaryConstructor(ctor.build()).build()
    }

    private data class SdField(val name: String, val isList: Boolean, val shortDesc: String?)

    private fun StructureDefinition.sdFields(): List<SdField> =
        differential?.element
            ?.filter { it.id != id && (it.path?.value ?: "").count { c -> c == '.' } == 1 }
            ?.map { SdField(fieldName(it.path?.value ?: ""), it.max?.value == "*", it.short?.value) }
            ?: emptyList()

    private inline fun <reified T> decodePayload(file: File): T? = try {
        val b64 = json.decodeFromString<JsonObject>(file.readText())["data"]?.jsonPrimitive?.content ?: return null
        json.decodeFromString<T>(String(Base64.getDecoder().decode(b64)))
    } catch (e: Exception) {
        println("Skipping ${file.name}: ${e.message}")
        null
    }

    private fun decodeRawPayload(file: File): JsonObject? = try {
        val b64 = json.decodeFromString<JsonObject>(file.readText())["data"]?.jsonPrimitive?.content ?: return null
        json.decodeFromString<JsonObject>(String(Base64.getDecoder().decode(b64)))
    } catch (e: Exception) {
        println("Skipping ${file.name}: ${e.message}")
        null
    }
}
