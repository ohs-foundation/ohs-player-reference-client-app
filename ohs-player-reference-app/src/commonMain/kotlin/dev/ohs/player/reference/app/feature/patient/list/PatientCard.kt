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
package dev.ohs.player.reference.app.feature.patient.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.ohs.player.reference.app.data.model.PatientView
import dev.ohs.player.reference.app.feature.component.common.CardView
import dev.ohs.player.reference.app.feature.component.common.StatusChip

data class PatientCardConfig(
  val showStatusChip: Boolean = true,
  val showGender: Boolean = true,
  val showBirthDate: Boolean = true,
  val showLastVisit: Boolean = true,
  val elevationDp: Float = 2f,
  val contentPaddingDp: Float = 16f,
)

@Composable
fun PatientCard(
  patient: PatientView,
  config: PatientCardConfig = PatientCardConfig(),
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  CardView(
    elevationDp = config.elevationDp,
    contentPaddingDp = config.contentPaddingDp,
    onClick = onClick,
  ) {
    header {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(patient.fullName, style = MaterialTheme.typography.titleMedium)
        if (config.showStatusChip) {
          StatusChip(isActive = patient.isActive)
        }
      }
    }
    body {
      val details = buildList {
        if (config.showGender) add(patient.gender)
        if (config.showBirthDate) add("Born: ${patient.birthDate}")
      }
      if (details.isNotEmpty()) {
        Text(
          text = details.joinToString("  •  "),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (config.showLastVisit) {
        Text(
          text = "Last visit: ${patient.lastVisitDate}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
