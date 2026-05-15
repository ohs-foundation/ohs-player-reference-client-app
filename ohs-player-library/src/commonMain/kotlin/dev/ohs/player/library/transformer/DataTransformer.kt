/*
 * Copyright 2026 Open Health Stack Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.ohs.player.library.transformer

import dev.ohs.fhir.fhirpath.FhirPathEngine
import dev.ohs.fhir.model.r4.Resource
import dev.ohs.player.library.domain.model.ViewDefinition
import dev.ohs.player.library.util.JsonUtil
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Transforms a FHIR [Resource] into a Kotlin data class [T] driven by a [ViewDefinition].
 *
 * Evaluates each FhirPath expression declared in the ViewDefinition columns against the provided
 * resource, then decodes the resulting values into an instance of [T].
 *
 * Example:
 * ```kotlin
 * val transformer = DataTransformer.forR4()
 *
 * val state: PatientHeaderState = transformer.transform(
 *     resource = patient,
 *     viewDefinition = ViewDefinition(
 *         name = "patient_header",
 *         resource = "Patient",
 *         select = listOf(SelectBlock(column = listOf(
 *             ViewColumn(name = "patientId",  path = "id"),
 *             ViewColumn(name = "familyName", path = "name.family.first()")
 *         )))
 *     )
 * )
 * // state.patientId  → "P-001"
 * // state.familyName → "Smith"
 * ```
 */
class DataTransformer(private val fhirPathEngine: FhirPathEngine) {
  /**
   * Evaluates each FhirPath column declared in [viewDefinition] against [resource] and decodes the
   * result into an instance of [T].
   *
   * @param resource the FHIR resource to extract data from
   * @param viewDefinition declares the columns (name → FhirPath) to evaluate
   * @param contextVariablesMap optional named variables available during FhirPath evaluation
   * @return an instance of [T] with fields populated from the extracted values
   */
  suspend inline fun <reified T : Any> transform(
    resource: Resource,
    viewDefinition: ViewDefinition,
    contextVariablesMap: Map<String, Any> = emptyMap(),
  ): T {
    val jsonElements =
      extract(resource, viewDefinition, contextVariablesMap).mapValues { (_, value) ->
        if (value != null) JsonPrimitive(value.toString()) else JsonNull
      }
    return JsonUtil.json.decodeFromJsonElement<T>(JsonObject(jsonElements))
  }

  /**
   * Evaluates each FhirPath column in [viewDefinition] against [resource] and returns the raw
   * extracted values as a map.
   *
   * Columns with a null name or path are silently skipped.
   *
   * @param resource the FHIR resource to extract data from
   * @param viewDefinition declares the columns to evaluate
   * @param contextVariablesMap optional named variables for FhirPath evaluation
   * @return a map of column name to extracted value; values are null if the path yields no result
   *   or evaluation fails
   */
  suspend fun extract(
    resource: Resource,
    viewDefinition: ViewDefinition,
    contextVariablesMap: Map<String, Any> = emptyMap(),
  ): Map<String, Any?> =
    viewDefinition
      .allColumns()
      .filter { it.name != null && it.path != null }
      .associate { column ->
        column.name!! to evaluatePath(resource, column.path!!, contextVariablesMap)
      }

  private suspend fun evaluatePath(
    focus: Any?,
    path: String,
    contextVariablesMap: Map<String, Any>,
  ): Any? =
    runCatching {
        fhirPathEngine
          .evaluateExpression(expression = path, base = focus, variables = contextVariablesMap)
          .firstOrNull()
      }
      .getOrNull()

  companion object {
    /** Creates a [DataTransformer] configured for FHIR R4. */
    fun forR4(): DataTransformer = DataTransformer(FhirPathEngine.forR4())
  }
}
