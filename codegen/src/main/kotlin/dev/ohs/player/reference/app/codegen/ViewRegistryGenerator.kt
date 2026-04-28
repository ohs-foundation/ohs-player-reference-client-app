package dev.ohs.player.reference.app.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.Base64

/**
 * Generates a static [viewDefinitions] map from Binary-*View.json files.
 * The map type mirrors the library's ViewDefinition/SelectBlock/ViewColumn.
 */
object ViewRegistryGenerator {

    private val json = Json { ignoreUnknownKeys = true }

    private val viewDefinitionClass = ClassName("dev.ohs.player.library.config", "ViewDefinition")
    private val selectBlockClass = ClassName("dev.ohs.player.library.config", "SelectBlock")
    private val viewColumnClass = ClassName("dev.ohs.player.library.config", "ViewColumn")
    private val stringToViewDefMap = ClassName("kotlin.collections", "Map")
        .parameterizedBy(STRING, viewDefinitionClass)

    fun generate(igOutputDir: File, fshResourceDir: File, outputPackage: String): FileSpec? {
        val viewFiles = igOutputDir.listFiles()
            ?.filter { it.name.startsWith("Binary-") && it.name.endsWith("View.json") }
            ?: return null

        val entries = viewFiles.mapNotNull { readEntry(it) }
        if (entries.isEmpty()) return null

        val mapCode = CodeBlock.builder().add("mapOf(\n")
        entries.forEachIndexed { i, (id, vd) ->
            mapCode.add("  %S to %T(\n", id, viewDefinitionClass)
            mapCode.add("    name = %S,\n", vd.name)
            mapCode.add("    resource = %S,\n", vd.resource)
            mapCode.add("    select = listOf(\n")
            vd.selects.forEach { select ->
                mapCode.add("      %T(column = listOf(\n", selectBlockClass)
                select.forEach { col ->
                    mapCode.add("        %T(name = %S, path = %S),\n", viewColumnClass, col.first, col.second)
                }
                mapCode.add("      )),\n")
            }
            mapCode.add("    )\n")
            mapCode.add("  )%L\n", if (i < entries.lastIndex) "," else "")
        }
        mapCode.add(")")

        return FileSpec.builder(outputPackage, "ViewRegistry")
            .addProperty(
                PropertySpec.builder("viewDefinitions", stringToViewDefMap)
                    .initializer(mapCode.build())
                    .build()
            )
            .build()
    }

    private data class ViewDefData(
        val name: String,
        val resource: String,
        val selects: List<List<Pair<String, String>>>
    )

    private fun readEntry(file: File): Pair<String, ViewDefData>? = try {
        val binary = json.decodeFromString<JsonObject>(file.readText())
        val b64 = binary["data"]?.jsonPrimitive?.content ?: return null
        val vdJson = json.decodeFromString<JsonObject>(String(Base64.getDecoder().decode(b64)))

        val instanceId = file.name.removePrefix("Binary-").removeSuffix(".json")
        val name = vdJson["name"]?.jsonPrimitive?.content ?: return null
        val resource = vdJson["resource"]?.jsonPrimitive?.content ?: return null
        val selects = vdJson["select"]?.jsonArray?.map { selectEl ->
            selectEl.jsonObject["column"]?.jsonArray?.map { col ->
                val colObj = col.jsonObject
                val colName = colObj["name"]?.jsonPrimitive?.content ?: ""
                val colPath = colObj["path"]?.jsonPrimitive?.content ?: ""
                colName to colPath
            } ?: emptyList()
        } ?: emptyList()

        instanceId to ViewDefData(name, resource, selects) // We'll keep instanceId as variable name but it's not used as key
        name to ViewDefData(name, resource, selects)
    } catch (e: Exception) {
        println("Skipping ${file.name}: ${e.message}")
        null
    }
}
