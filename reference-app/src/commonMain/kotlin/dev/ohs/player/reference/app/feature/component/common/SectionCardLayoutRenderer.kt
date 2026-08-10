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
package dev.ohs.player.reference.app.feature.component.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ohs.player.client.renderer.ConfiguredRenderer
import dev.ohs.player.client.renderer.LayoutRenderer
import dev.ohs.player.client.renderer.RenderOptions
import dev.ohs.player.generated.config.SectionCardConfig
import org.jetbrains.compose.resources.stringResource
import player_reference.reference_app.generated.resources.Res
import player_reference.reference_app.generated.resources.section_collapse
import player_reference.reference_app.generated.resources.section_expand

/**
 * Layout renderer that introduces a list of items with a flat overline section label — no card,
 * border, or per-row divider. Registered under
 * [dev.ohs.player.generated.viewtype.ViewTypeCS.SectionCard].
 *
 * Supports an optional item count and collapsible behavior driven by [SectionCardConfig].
 */
class SectionCardLayoutRenderer<T>(
  private val title: String,
  private val icon: ImageVector,
  private val iconTint: Color? = null,
  private val config: SectionCardConfig = SectionCardConfig(),
) : LayoutRenderer<T> {

  @Composable
  override fun Render(
    items: List<T>,
    component: ConfiguredRenderer<T>,
    key: ((T) -> Any)?,
    onItemClick: (T) -> Unit,
    modifier: Modifier,
  ) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val tint = iconTint ?: MaterialTheme.colorScheme.primary
    val collapsible = config.collapsible == true

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .then(if (collapsible) Modifier.clickable { expanded = !expanded } else Modifier)
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = tint,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = title.uppercase(),
          style = MaterialTheme.typography.labelLarge,
          letterSpacing = 0.8.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
        )
        if (config.showItemCount != false) {
          Text(
            text = items.size.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (collapsible) {
          Spacer(modifier = Modifier.width(12.dp))
          val toggleLabel =
            stringResource(if (expanded) Res.string.section_collapse else Res.string.section_expand)
          val barColor = MaterialTheme.colorScheme.onSurfaceVariant
          Box(
            modifier = Modifier.size(20.dp).semantics { contentDescription = toggleLabel },
            contentAlignment = Alignment.Center,
          ) {
            Box(
              Modifier.width(12.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(barColor)
            )
            if (!expanded) {
              Box(
                Modifier.width(2.dp)
                  .height(12.dp)
                  .clip(RoundedCornerShape(1.dp))
                  .background(barColor)
              )
            }
          }
        }
      }

      AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(),
        exit = shrinkVertically(),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          HorizontalDivider(
            modifier = Modifier.padding(bottom = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
          )
          items.forEach { item ->
            component.Render(item, RenderOptions(onClick = { onItemClick(item) }))
          }
        }
      }
    }
  }
}
