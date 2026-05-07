package dev.ohs.player.reference.app.codegen

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.STRING
import dev.ohs.fhir.model.r4.CodeSystem
import dev.ohs.fhir.model.r4.FhirR4Json
import dev.ohs.fhir.model.r4.StructureDefinition
import kotlinx.serialization.Serializable
import java.io.File

val fhirJson = FhirR4Json { ignoreUnknownKeys = true }

fun parseSd(file: File): StructureDefinition = fhirJson.decodeFromString(file.readText()) as StructureDefinition

fun parseCodeSystem(file: File): CodeSystem = fhirJson.decodeFromString(file.readText()) as CodeSystem

/** Returns the last path segment as a field name: `ViewConfig.padding` → `padding`. */
fun fieldName(path: String): String = path.substringAfterLast('.')

fun kotlinTypeFor(fhirType: String?) = when (fhirType) {
    "decimal" -> ClassName("kotlin", "Double")
    "integer" -> INT
    "boolean" -> BOOLEAN
    else -> STRING
}

// ---------------------------------------------------------------------------
// Decoded content models for Binary-*.json IG resources
// ---------------------------------------------------------------------------

/** Decoded payload from Binary-*View.json (a ViewDefinition logical model instance). */
@Serializable
data class ViewDefinitionData(
    val name: String,
    val url: String? = null,
    val resource: String,
    val select: List<SelectData> = emptyList()
) {
    fun allColumns(): List<ColumnData> = select.flatMap { it.column }
}

@Serializable
data class SelectData(val column: List<ColumnData> = emptyList())

@Serializable
data class ColumnData(val name: String, val path: String)
