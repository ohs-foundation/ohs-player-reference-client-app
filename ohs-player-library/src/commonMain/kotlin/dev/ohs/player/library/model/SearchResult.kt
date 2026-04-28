package dev.ohs.player.library.model

import dev.ohs.fhir.model.r4.Resource

/**
 * Represents the result of a FHIR search query.
 */
data class SearchResult<R : Resource>(
  val resource: R,
  val included: Map<String, List<Resource>>? = null,
  val revIncluded: Map<String, List<Resource>>? = null
) {
  /**
   * Flat list of ALL resources (primary + included + revIncluded) for FHIRPath context.
   * The primary resource is included so cross-resource FHIRPath expressions can resolve it.
   */
  fun allResources(): List<Resource> {
    return buildList {
      add(resource)
      included?.values?.flatten()?.let { addAll(it) }
      revIncluded?.values?.flatten()?.let { addAll(it) }
    }
  }
}