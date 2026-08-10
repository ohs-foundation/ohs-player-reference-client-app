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
package dev.ohs.player.reference.app.feature.group.list

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ohs.player.generated.config.GroupCardConfig
import dev.ohs.player.generated.state.GroupListState
import org.jetbrains.compose.resources.stringResource
import player_reference.reference_app.generated.resources.Res
import player_reference.reference_app.generated.resources.group_member_count_one
import player_reference.reference_app.generated.resources.group_member_count_other
import player_reference.reference_app.generated.resources.group_unknown_name

/**
 * Selected household id for the two-pane list-detail highlight; null when there is no open pane.
 */
val LocalSelectedGroupId = compositionLocalOf<String?> { null }

@Composable
fun GroupCard(
  group: GroupListState,
  config: GroupCardConfig = GroupCardConfig(),
  onClick: (() -> Unit)? = null,
) {
  val name = group.groupName ?: stringResource(Res.string.group_unknown_name)
  val initials = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "H"
  val count = group.memberCount ?: "0"
  val memberLabel =
    stringResource(
      if (count == "1") Res.string.group_member_count_one else Res.string.group_member_count_other,
      count,
    )
  val selected = group.groupId != null && group.groupId == LocalSelectedGroupId.current

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 12.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalAlignment = Alignment.CenterVertically,
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
        text = name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color =
          if (selected) MaterialTheme.colorScheme.onPrimaryContainer
          else MaterialTheme.colorScheme.onSurface,
      )
      if (config.showMemberCount != false) {
        Text(
          text = memberLabel,
          style = MaterialTheme.typography.bodyMedium,
          color =
            if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
