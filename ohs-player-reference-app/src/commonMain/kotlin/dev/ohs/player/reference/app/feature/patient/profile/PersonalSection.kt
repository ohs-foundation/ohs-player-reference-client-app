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
package dev.ohs.player.reference.app.feature.patient.profile

import androidx.compose.runtime.Composable
import dev.ohs.player.reference.app.data.model.PatientView
import dev.ohs.player.reference.app.feature.component.common.InfoRow
import dev.ohs.player.reference.app.feature.component.common.SectionCard

@Composable
fun PersonalSection(patient: PatientView) {
  SectionCard(title = "Personal Information") {
    InfoRow("Full Name", patient.fullName)
    InfoRow("Gender", patient.gender)
    InfoRow("Date of Birth", patient.birthDate)
    InfoRow("Phone", patient.phoneNumber)
    InfoRow("Address", patient.address.formatted)
    InfoRow("Status", if (patient.isActive) "Active" else "Inactive")
  }
}
