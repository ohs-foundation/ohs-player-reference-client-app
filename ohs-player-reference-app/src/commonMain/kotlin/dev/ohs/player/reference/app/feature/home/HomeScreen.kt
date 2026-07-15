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
package dev.ohs.player.reference.app.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ohs.player.reference.app.feature.group.list.GroupListScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  onGroupClick: (String) -> Unit,
  onDataCaptureClick: () -> Unit,
  onSyncNowClick: () -> Unit,
  lastSyncedAt: String?,
) {
  var selectedDestination by remember { mutableStateOf(HomeDestination.Households) }
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  BoxWithConstraints {
    val isExpandedWidth = isHomeDrawerExpandedWidth(maxWidth)

    fun closeDrawerIfCompact() {
      if (!isExpandedWidth) scope.launch { drawerState.close() }
    }

    val drawerItems: @Composable () -> Unit = {
      Column(modifier = Modifier.fillMaxHeight()) {
        Text(
          text = "Registers",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 8.dp),
        )
        HomeDestination.entries.forEach { destination ->
          NavigationDrawerItem(
            label = { Text(destination.label) },
            icon = { Icon(destination.icon, contentDescription = null) },
            selected = destination == selectedDestination,
            onClick = {
              selectedDestination = destination
              closeDrawerIfCompact()
            },
          )
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider()
        if (lastSyncedAt != null) {
          Text(
            text = "Last synced: $lastSyncedAt",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 28.dp, top = 8.dp),
          )
        }
        NavigationDrawerItem(
          label = { Text("Sync now") },
          icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
          selected = false,
          onClick = {
            onSyncNowClick()
            closeDrawerIfCompact()
          },
        )
      }
    }

    val content: @Composable () -> Unit = {
      when (selectedDestination) {
        HomeDestination.Households ->
          GroupListScreen(onGroupClick = onGroupClick, onDataCaptureClick = onDataCaptureClick)
      }
    }

    if (isExpandedWidth) {
      PermanentNavigationDrawer(drawerContent = { PermanentDrawerSheet { drawerItems() } }) {
        content()
      }
    } else {
      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { ModalDrawerSheet { drawerItems() } },
      ) {
        Scaffold(
          topBar = {
            TopAppBar(
              title = { Text(selectedDestination.label) },
              navigationIcon = {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                  Icon(Icons.Filled.Menu, contentDescription = "Open navigation menu")
                }
              },
            )
          }
        ) { padding ->
          Box(modifier = Modifier.padding(padding)) { content() }
        }
      }
    }
  }
}
