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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ohs.player.client.renderer.ComponentRenderer
import dev.ohs.player.client.renderer.RenderOptions
import dev.ohs.player.generated.config.PatientHeaderConfig
import dev.ohs.player.generated.state.PatientSummaryState
import dev.ohs.player.reference.app.feature.component.common.StatusChip
import dev.ohs.player.reference.app.feature.patient.list.calculateAge
import org.jetbrains.compose.resources.stringResource
import player_reference.reference_app.generated.resources.Res
import player_reference.reference_app.generated.resources.label_age
import player_reference.reference_app.generated.resources.label_mrn
import player_reference.reference_app.generated.resources.name_unknown

class PatientHeaderRenderer : ComponentRenderer<PatientSummaryState, PatientHeaderConfig> {
  @Composable
  override fun Render(
    item: PatientSummaryState,
    config: PatientHeaderConfig,
    options: RenderOptions,
  ) {
    PatientHeaderCard(patient = item, config = config, modifier = options.modifier)
  }
}

@Composable
fun PatientHeaderCard(
  patient: PatientSummaryState,
  config: PatientHeaderConfig = PatientHeaderConfig(),
  modifier: Modifier = Modifier,
) {
  val initials =
    buildString {
        patient.givenName?.firstOrNull()?.uppercaseChar()?.let { append(it) }
        patient.familyName?.firstOrNull()?.uppercaseChar()?.let { append(it) }
      }
      .ifBlank { "?" }
  val fullName =
    listOfNotNull(patient.givenName, patient.familyName).joinToString(" ").ifBlank {
      stringResource(Res.string.name_unknown)
    }
  val meta =
    buildList {
        calculateAge(patient.birthDate?.toString())?.let {
          add(stringResource(Res.string.label_age, it))
        }
        if (config.showGender != false) {
          patient.gender?.let { add(it.replaceFirstChar { c -> c.uppercaseChar() }) }
        }
        if (config.showMrn != false)
          patient.mrn?.let { add(stringResource(Res.string.label_mrn, it)) }
        patient.phone?.let { add(it) }
      }
      .joinToString(" · ")

  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = initials,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.Bold,
      )
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = fullName,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
      if (meta.isNotEmpty()) {
        Text(
          text = meta,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (config.showStatus != false) {
        StatusChip(isActive = patient.active ?: false)
      }
    }
  }
}
