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

import dev.ohs.player.reference.app.data.datasource.PATIENTS_JSON
import dev.ohs.player.reference.app.data.model.PatientView
import kotlinx.serialization.json.Json

object PatientRepository {

  private val json = Json { ignoreUnknownKeys = true }

  private val patients: List<PatientView> by lazy {
    json.decodeFromString<List<PatientView>>(PATIENTS_JSON)
  }

  fun getAll(): List<PatientView> = patients

  fun getById(id: String): PatientView? = patients.find { it.id == id }
}
