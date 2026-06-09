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
import dev.ohs.player.generated.config.AllergyItemConfig
import dev.ohs.player.generated.state.PatientAllergyState
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.library.renderer.RenderOptions
import dev.ohs.player.reference.app.feature.component.common.Chip

class AllergyItemRenderer : ComponentRenderer<PatientAllergyState, AllergyItemConfig> {
  @Composable
  override fun Render(
    item: PatientAllergyState,
    config: AllergyItemConfig,
    options: RenderOptions,
  ) {
    AllergyItemRow(item = item, config = config, modifier = options.modifier)
  }
}

@Composable
fun AllergyItemRow(
  item: PatientAllergyState,
  config: AllergyItemConfig = AllergyItemConfig(),
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .background(
          if (item.criticality?.lowercase() == "high")
            criticalityColor(item.criticality).copy(alpha = 0.06f)
          else Color.Transparent
        )
        .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    if (config.showCriticality != false) {
      Box(
        modifier =
          Modifier.width(3.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(criticalityColor(item.criticality))
      )
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = item.substance ?: "Unknown substance",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
      )
      if (config.showCriticality != false) {
        item.criticality?.let {
          Text(
            text = it.replaceFirstChar { c -> c.uppercaseChar() },
            style = MaterialTheme.typography.bodySmall,
            color = criticalityColor(it).copy(alpha = 0.85f),
          )
        }
      }
    }
    if (config.showStatus != false) {
      item.allergyStatus?.let { status ->
        Chip(
          label = status.replaceFirstChar { it.uppercaseChar() },
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
    }
  }
}

@Composable
fun criticalityColor(criticality: String?): Color =
  when (criticality?.lowercase()) {
    "high" -> MaterialTheme.colorScheme.error
    "moderate" -> Color(0xFFE37400)
    "low" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }
