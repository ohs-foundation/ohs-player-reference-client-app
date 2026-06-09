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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ohs.player.generated.config.MedicationItemConfig
import dev.ohs.player.generated.state.PatientMedicationState
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.library.renderer.RenderOptions
import dev.ohs.player.reference.app.feature.component.common.Chip

class MedicationItemRenderer : ComponentRenderer<PatientMedicationState, MedicationItemConfig> {
  @Composable
  override fun Render(
    item: PatientMedicationState,
    config: MedicationItemConfig,
    options: RenderOptions,
  ) {
    MedicationItemRow(item = item, config = config, modifier = options.modifier)
  }
}

@Composable
fun MedicationItemRow(
  item: PatientMedicationState,
  config: MedicationItemConfig = MedicationItemConfig(),
  modifier: Modifier = Modifier,
) {
  val isStopped = item.medStatus?.lowercase() == "stopped"

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .background(
          if (isStopped) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
          else Color.Transparent
        )
        .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(
      modifier =
        Modifier.width(3.dp)
          .height(36.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(
            if (isStopped) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
          )
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = item.medicationName ?: "Unknown medication",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
      )
      if (config.showDosage != false) {
        item.dosage?.let {
          Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
    if (config.showStatus != false) {
      item.medStatus?.let { status ->
        val (bg, fg) =
          when (status.lowercase()) {
            "active" ->
              MaterialTheme.colorScheme.primaryContainer to
                MaterialTheme.colorScheme.onPrimaryContainer
            "stopped" ->
              MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
            else ->
              MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
          }
        Chip(
          label = status.replaceFirstChar { it.uppercaseChar() },
          containerColor = bg,
          contentColor = fg,
        )
      }
    }
  }
}
