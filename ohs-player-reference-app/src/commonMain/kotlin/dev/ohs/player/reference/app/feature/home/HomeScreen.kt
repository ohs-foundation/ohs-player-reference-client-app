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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import dev.ohs.player.reference.app.feature.group.list.GroupListScreen
import dev.ohs.player.reference.app.feature.group.profile.GroupProfileScreen
import kotlinx.coroutines.launch
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.Res
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_cancel_sync
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_last_synced
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_open_navigation_menu
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_registers
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_select_household
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_sign_out
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_sync_cancelled
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_sync_failed
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_sync_in_progress
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_sync_now
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomeScreen(
  onGroupClick: (String) -> Unit,
  onDataCaptureClick: () -> Unit,
  onMemberClick: (String) -> Unit,
  onAddMembers: (String) -> Unit,
  onSignOut: () -> Unit,
) {
  val homeViewModel: HomeViewModel = koinViewModel()
  val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

  var selectedDestination by remember { mutableStateOf(HomeDestination.Households) }
  var selectedGroupId by rememberSaveable { mutableStateOf<String?>(null) }
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val syncErrorMessage =
    when (uiState.syncError) {
      SyncError.Failed -> stringResource(Res.string.home_sync_failed)
      SyncError.Cancelled -> stringResource(Res.string.home_sync_cancelled)
      null -> null
    }
  LaunchedEffect(uiState.syncError) {
    if (syncErrorMessage != null) {
      snackbarHostState.showSnackbar(syncErrorMessage)
      homeViewModel.clearSyncError()
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    val isExpandedWidth =
      currentWindowAdaptiveInfo()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    fun closeDrawerIfCompact() {
      if (!isExpandedWidth) scope.launch { drawerState.close() }
    }

    val drawerItems: @Composable () -> Unit = {
      val syncInProgressDescription = stringResource(Res.string.home_sync_in_progress)
      Column(modifier = Modifier.fillMaxHeight()) {
        Text(
          text = stringResource(Res.string.home_registers),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 8.dp),
        )
        HomeDestination.entries.forEach { destination ->
          NavigationDrawerItem(
            label = { Text(stringResource(destination.label)) },
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
        uiState.lastSyncedAt?.let { lastSyncedAt ->
          Text(
            text = stringResource(Res.string.home_last_synced, lastSyncedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 28.dp, top = 8.dp),
          )
        }
        NavigationDrawerItem(
          label = {
            Text(
              stringResource(
                if (uiState.isSyncing) Res.string.home_cancel_sync else Res.string.home_sync_now
              )
            )
          },
          icon = {
            Icon(
              if (uiState.isSyncing) Icons.Filled.Close else Icons.Filled.Refresh,
              contentDescription = null,
            )
          },
          badge = {
            if (uiState.isSyncing) {
              CircularProgressIndicator(
                modifier =
                  Modifier.size(16.dp).semantics { contentDescription = syncInProgressDescription },
                strokeWidth = 2.dp,
              )
            }
          },
          selected = false,
          onClick = {
            if (uiState.isSyncing) {
              homeViewModel.cancelSync()
            } else {
              homeViewModel.syncNow()
            }
            closeDrawerIfCompact()
          },
        )
        NavigationDrawerItem(
          label = { Text(stringResource(Res.string.home_sign_out)) },
          icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
          selected = false,
          onClick = {
            onSignOut()
            closeDrawerIfCompact()
          },
        )
      }
    }

    val content: @Composable () -> Unit = {
      when (selectedDestination) {
        HomeDestination.Households ->
          if (isExpandedWidth) {
            Row(modifier = Modifier.fillMaxSize()) {
              Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                GroupListScreen(
                  onGroupClick = { selectedGroupId = it },
                  onDataCaptureClick = onDataCaptureClick,
                )
              }
              VerticalDivider()
              Box(modifier = Modifier.weight(1.5f).fillMaxSize()) {
                val groupId = selectedGroupId
                if (groupId != null) {
                  GroupProfileScreen(
                    groupId = groupId,
                    onBack = { selectedGroupId = null },
                    onMemberClick = onMemberClick,
                    onAddMembers = { onAddMembers(groupId) },
                  )
                } else {
                  EmptyDetailPlaceholder()
                }
              }
            }
          } else {
            GroupListScreen(onGroupClick = onGroupClick, onDataCaptureClick = onDataCaptureClick)
          }
      }
    }

    if (isExpandedWidth) {
      PermanentNavigationDrawer(drawerContent = { PermanentDrawerSheet { drawerItems() } }) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
          Box(modifier = Modifier.padding(padding)) { content() }
        }
      }
    } else {
      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { ModalDrawerSheet { drawerItems() } },
      ) {
        Scaffold(
          topBar = {
            TopAppBar(
              title = { Text(stringResource(selectedDestination.label)) },
              navigationIcon = {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                  Icon(
                    Icons.Filled.Menu,
                    contentDescription = stringResource(Res.string.home_open_navigation_menu),
                  )
                }
              },
            )
          },
          snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
          Box(modifier = Modifier.padding(padding)) { content() }
        }
      }
    }
  }
}

@Composable
private fun EmptyDetailPlaceholder() {
  Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
    Text(
      text = stringResource(Res.string.home_select_household),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}
