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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ohs.player.reference.app.data.model.PatientView
import dev.ohs.player.reference.app.feature.component.common.Chip
import dev.ohs.player.reference.app.feature.component.common.EmptyStateText
import dev.ohs.player.reference.app.feature.component.common.InfoRow
import dev.ohs.player.reference.app.feature.component.common.SectionCard

@Composable
fun MedicalSection(patient: PatientView) {
  SectionCard(title = "Medical Information") {
    InfoRow("Blood Type", patient.bloodType)
    InfoRow("MRN", patient.medicalRecordNumber)
    InfoRow("Last Visit", patient.lastVisitDate)
    HorizontalDivider(Modifier.padding(vertical = 8.dp))

    Text("Allergies", style = MaterialTheme.typography.labelMedium)
    if (patient.allergies.isEmpty()) {
      EmptyStateText()
    } else {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        patient.allergies.forEach { allergy ->
          Chip(
            label = allergy,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
      }
    }

    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    Text("Conditions", style = MaterialTheme.typography.labelMedium)
    if (patient.conditions.isEmpty()) {
      EmptyStateText()
    } else {
      patient.conditions.forEach { condition ->
        Text("•  $condition", style = MaterialTheme.typography.bodyMedium)
      }
    }

    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    Text("Medications", style = MaterialTheme.typography.labelMedium)
    if (patient.medications.isEmpty()) {
      EmptyStateText()
    } else {
      patient.medications.forEach { med ->
        Column(Modifier.padding(vertical = 4.dp)) {
          Text(med.name, style = MaterialTheme.typography.bodyMedium)
          Text(
            "${med.dosage}, ${med.frequency}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
