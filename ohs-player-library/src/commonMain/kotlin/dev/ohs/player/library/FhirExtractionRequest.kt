package dev.ohs.player.library

import dev.ohs.fhir.model.r4.Resource

data class FhirExtractionRequest(
    val resource: Resource,
    val paths: Map<String, String>,
    val context: List<Resource> = emptyList(),
    val strict: Boolean = false
)