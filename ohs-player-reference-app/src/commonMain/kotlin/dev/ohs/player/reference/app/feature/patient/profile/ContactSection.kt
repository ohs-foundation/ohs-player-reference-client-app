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

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ohs.player.reference.app.data.model.PatientView
import dev.ohs.player.reference.app.feature.component.common.InfoRow
import dev.ohs.player.reference.app.feature.component.common.SectionCard

@Composable
fun ContactSection(patient: PatientView) {
  SectionCard(title = "Contact & Insurance") {
    Text("Emergency Contact", style = MaterialTheme.typography.labelMedium)
    InfoRow("Name", patient.emergencyContact.name)
    InfoRow("Relationship", patient.emergencyContact.relationship)
    InfoRow("Phone", patient.emergencyContact.phoneNumber)
    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    InfoRow("Insurance", patient.insuranceProvider)
  }
}
