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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import dev.ohs.player.generated.config.GroupHeaderConfig
import dev.ohs.player.generated.state.GroupHeaderState
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.library.renderer.RenderOptions
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.Res
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.group_default_name
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.group_head
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.group_member_count_one
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.group_member_count_other
import org.jetbrains.compose.resources.stringResource

class GroupHeaderRenderer : ComponentRenderer<GroupHeaderState, GroupHeaderConfig> {
  @Composable
  override fun Render(item: GroupHeaderState, config: GroupHeaderConfig, options: RenderOptions) {
    GroupHeaderCard(item = item, config = config, modifier = options.modifier)
  }
}

@Composable
fun GroupHeaderCard(
  item: GroupHeaderState,
  config: GroupHeaderConfig = GroupHeaderConfig(),
  modifier: Modifier = Modifier,
) {
  val name = item.groupName ?: stringResource(Res.string.group_default_name)
  val initials = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "H"
  val memberCount = item.memberCount ?: "0"
  val headName =
    listOfNotNull(item.headGivenName, item.headFamilyName).joinToString(" ").ifBlank { null }
  val meta =
    buildList {
        if (config.showMemberCount != false) {
          add(
            stringResource(
              if (memberCount == "1") Res.string.group_member_count_one
              else Res.string.group_member_count_other,
              memberCount,
            )
          )
        }
        if (config.showHeadName != false && headName != null) {
          add(stringResource(Res.string.group_head, headName))
        }
      }
      .joinToString(" · ")

  Row(
    modifier = modifier.fillMaxWidth().padding((config.padding?.floatValue() ?: 20f).dp),
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
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = name,
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
    }
  }
}
