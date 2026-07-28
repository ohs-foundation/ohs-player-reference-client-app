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
package dev.ohs.player.reference.app.data.repository

import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Resource
import kotlinx.coroutines.flow.StateFlow

/** Minimal persistence seam for FHIR resources, backed by [FhirEngineRepository] in production. */
interface FhirRepository {
  /**
   * Incremented on every successful [upsert]. Implementers must bump this after each write so that
   * observers (e.g. [PatientRepository], [GroupRepository]) know to re-query.
   *
   * TODO: To be deleted, once https://github.com/ohs-foundation/kotlin-fhir-engine/issues/65 is
   *   done
   */
  val revision: StateFlow<Long>

  suspend fun upsert(resource: Resource)

  suspend fun upsert(bundle: Bundle): Int

  suspend fun get(resourceType: String, id: String): Resource?

  suspend fun all(resourceType: String): List<Resource>

  /** True once any `Patient` or `Group` has been persisted locally. Drives the initial sync gate. */
  suspend fun hasAnyData(): Boolean
}
