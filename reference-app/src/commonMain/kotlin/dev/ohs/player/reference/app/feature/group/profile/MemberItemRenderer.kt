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
package dev.ohs.player.reference.app.feature.group.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
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
import dev.ohs.player.generated.config.MemberItemConfig
import dev.ohs.player.generated.state.GroupMemberState
import dev.ohs.player.reference.app.feature.component.common.Chip
import dev.ohs.player.reference.app.feature.patient.list.calculateAge
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import player_reference.reference_app.generated.resources.Res
import player_reference.reference_app.generated.resources.label_age
import player_reference.reference_app.generated.resources.name_unknown
import player_reference.reference_app.generated.resources.relationship_child
import player_reference.reference_app.generated.resources.relationship_guardian
import player_reference.reference_app.generated.resources.relationship_non_relative
import player_reference.reference_app.generated.resources.relationship_other_relative
import player_reference.reference_app.generated.resources.relationship_parent
import player_reference.reference_app.generated.resources.relationship_spouse

class MemberItemRenderer : ComponentRenderer<GroupMemberState, MemberItemConfig> {
  @Composable
  override fun Render(item: GroupMemberState, config: MemberItemConfig, options: RenderOptions) {
    MemberItemRow(
      item = item,
      config = config,
      onClick = options.onClick,
      modifier = options.modifier,
    )
  }
}

@Composable
fun MemberItemRow(
  item: GroupMemberState,
  config: MemberItemConfig = MemberItemConfig(),
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val initials =
    buildString {
        item.memberGivenName?.firstOrNull()?.uppercaseChar()?.let { append(it) }
        item.memberFamilyName?.firstOrNull()?.uppercaseChar()?.let { append(it) }
      }
      .ifBlank { "?" }
  val fullName =
    listOfNotNull(item.memberGivenName, item.memberFamilyName).joinToString(" ").ifBlank {
      stringResource(Res.string.name_unknown)
    }
  val relationshipLabel = item.relationshipCode?.toRelationshipLabel()

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier =
        Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = initials,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.Bold,
      )
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = fullName,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
      )
      val subtitleParts = buildList {
        if (config.showAge != false) {
          calculateAge(item.memberBirthDate?.toString())?.let {
            add(stringResource(Res.string.label_age, it))
          }
        }
        if (config.showGender != false) {
          item.memberGender?.let { add(it.replaceFirstChar { c -> c.uppercaseChar() }) }
        }
      }
      if (subtitleParts.isNotEmpty()) {
        Text(
          text = subtitleParts.joinToString(" · "),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    if (relationshipLabel != null && config.showRelationship != false) {
      Chip(
        label = relationshipLabel,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
    if (onClick != null) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

private fun String.toRelationshipLabelResource(): StringResource? =
  when (uppercase()) {
    "SPS",
    "SPOUSE" -> Res.string.relationship_spouse
    "CHLD",
    "CHILD" -> Res.string.relationship_child
    "PRN",
    "PARENT" -> Res.string.relationship_parent
    "GUAR",
    "GUARDIAN" -> Res.string.relationship_guardian
    "EXT",
    "RELATIVE" -> Res.string.relationship_other_relative
    "FRND",
    "NON-RELATIVE" -> Res.string.relationship_non_relative
    else -> null
  }

@Composable
private fun String.toRelationshipLabel(): String =
  toRelationshipLabelResource()?.let { stringResource(it) }
    ?: lowercase()
      .split('-', '_')
      .filter { it.isNotBlank() }
      .joinToString(" ") { token -> token.replaceFirstChar { it.uppercaseChar() } }
      .ifBlank { this }
