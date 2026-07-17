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
package dev.ohs.player.reference.app.data

internal fun patientIdFromReference(reference: String?): String? {
  val normalizedReference = reference?.substringBefore("/_history/")?.trim().orEmpty()
  if (normalizedReference.isBlank()) return null

  return when {
    normalizedReference.startsWith("Patient/") ->
      normalizedReference.removePrefix("Patient/").ifBlank { null }
    normalizedReference.contains("/Patient/") ->
      normalizedReference.substringAfterLast("/Patient/").ifBlank { null }
    normalizedReference.startsWith("urn:uuid:") ->
      normalizedReference.substringAfterLast(':').ifBlank { null }
    '/' in normalizedReference -> normalizedReference.substringAfterLast('/').ifBlank { null }
    else -> normalizedReference
  }
}
