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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import dev.ohs.player.reference.app.feature.group.list.GroupListScreen
import dev.ohs.player.reference.app.feature.group.list.LocalSelectedGroupId
import dev.ohs.player.reference.app.feature.group.profile.GroupProfileScreen
import dev.ohs.player.reference.app.feature.patient.profile.PatientProfileScreen
import kotlinx.coroutines.launch
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.Res
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_cancel_sync
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_last_synced
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_open_navigation_menu
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_registers
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_select_household
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_sign_out
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_signed_in
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_sync_cancelled
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_sync_failed
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_sync_in_progress
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_sync_now
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomeScreen(
  userName: String,
  onGroupClick: (String) -> Unit,
  onDataCaptureClick: () -> Unit,
  onAddMembers: (String) -> Unit,
  onAddClinicalData: (String) -> Unit,
  onSignOut: () -> Unit,
) {
  val homeViewModel: HomeViewModel = koinViewModel()
  val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

  var selectedDestination by remember { mutableStateOf(HomeDestination.Households) }
  var selectedGroupId by rememberSaveable { mutableStateOf<String?>(null) }
  var selectedPatientId by rememberSaveable(selectedGroupId) { mutableStateOf<String?>(null) }
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
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isExpandedWidth =
      windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val isMediumWidth =
      !isExpandedWidth &&
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    fun closeDrawerIfCompact() {
      if (!isExpandedWidth) scope.launch { drawerState.close() }
    }

    val onDrawer = MaterialTheme.colorScheme.onPrimary
    val drawerItemColors =
      NavigationDrawerItemDefaults.colors(
        selectedContainerColor = onDrawer.copy(alpha = 0.20f),
        unselectedContainerColor = Color.Transparent,
        selectedTextColor = onDrawer,
        unselectedTextColor = onDrawer,
        selectedIconColor = onDrawer,
        unselectedIconColor = onDrawer,
      )
    val drawerItems: @Composable () -> Unit = {
      val syncInProgressDescription = stringResource(Res.string.home_sync_in_progress)
      Column(modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp)) {
        Row(
          modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 20.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.size(40.dp).clip(CircleShape).background(onDrawer.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
              style = MaterialTheme.typography.titleMedium,
              color = onDrawer,
            )
          }
          Text(
            text = userName.ifBlank { stringResource(Res.string.home_signed_in) },
            style = MaterialTheme.typography.titleMedium,
            color = onDrawer,
            fontWeight = FontWeight.SemiBold,
          )
        }
        Text(
          text = stringResource(Res.string.home_registers),
          style = MaterialTheme.typography.titleSmall,
          color = onDrawer.copy(alpha = 0.7f),
          modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        HomeDestination.entries.forEach { destination ->
          NavigationDrawerItem(
            label = { Text(stringResource(destination.label)) },
            icon = { Icon(destination.icon, contentDescription = null) },
            selected = destination == selectedDestination,
            colors = drawerItemColors,
            onClick = {
              selectedDestination = destination
              closeDrawerIfCompact()
            },
          )
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(color = onDrawer.copy(alpha = 0.2f))
        uiState.lastSyncedAt?.let { lastSyncedAt ->
          Text(
            text = stringResource(Res.string.home_last_synced, lastSyncedAt),
            style = MaterialTheme.typography.bodySmall,
            color = onDrawer.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
          )
        }
        NavigationDrawerItem(
          colors = drawerItemColors,
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
                color = onDrawer,
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
          colors = drawerItemColors,
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
                CompositionLocalProvider(LocalSelectedGroupId provides selectedGroupId) {
                  GroupListScreen(
                    onGroupClick = { selectedGroupId = it },
                    onDataCaptureClick = onDataCaptureClick,
                  )
                }
              }
              VerticalDivider()
              Box(modifier = Modifier.weight(1.5f).fillMaxSize()) {
                val patientId = selectedPatientId
                val groupId = selectedGroupId
                when {
                  patientId != null ->
                    PatientProfileScreen(
                      patientId = patientId,
                      onBack = { selectedPatientId = null },
                      onAddClinicalData = { onAddClinicalData(patientId) },
                    )
                  groupId != null ->
                    GroupProfileScreen(
                      groupId = groupId,
                      onBack = { selectedGroupId = null },
                      onMemberClick = { selectedPatientId = it },
                      onAddMembers = { onAddMembers(groupId) },
                    )
                  else -> EmptyDetailPlaceholder()
                }
              }
            }
          } else {
            GroupListScreen(onGroupClick = onGroupClick, onDataCaptureClick = onDataCaptureClick)
          }
      }
    }

    if (isExpandedWidth) {
      Row(modifier = Modifier.fillMaxSize()) {
        Surface(
          modifier = Modifier.width(260.dp).fillMaxHeight(),
          color = MaterialTheme.colorScheme.primary,
        ) {
          drawerItems()
        }
        VerticalDivider()
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
          Box(modifier = Modifier.padding(padding)) { content() }
        }
      }
    } else if (isMediumWidth) {
      Row(modifier = Modifier.fillMaxSize()) {
        val railItemColors =
          NavigationRailItemDefaults.colors(
            indicatorColor = onDrawer.copy(alpha = 0.20f),
            selectedIconColor = onDrawer,
            unselectedIconColor = onDrawer,
            selectedTextColor = onDrawer,
            unselectedTextColor = onDrawer,
          )
        NavigationRail(containerColor = MaterialTheme.colorScheme.primary) {
          val syncInProgressDescription = stringResource(Res.string.home_sync_in_progress)
          HomeDestination.entries.forEach { destination ->
            NavigationRailItem(
              selected = destination == selectedDestination,
              onClick = { selectedDestination = destination },
              colors = railItemColors,
              icon = { Icon(destination.icon, contentDescription = null) },
              label = { Text(stringResource(destination.label)) },
            )
          }
          Spacer(modifier = Modifier.weight(1f))
          NavigationRailItem(
            selected = false,
            colors = railItemColors,
            onClick = {
              if (uiState.isSyncing) homeViewModel.cancelSync() else homeViewModel.syncNow()
            },
            icon = {
              if (uiState.isSyncing) {
                Icon(
                  Icons.Filled.Close,
                  contentDescription = null,
                  modifier = Modifier.semantics { contentDescription = syncInProgressDescription },
                )
              } else {
                Icon(Icons.Filled.Refresh, contentDescription = null)
              }
            },
            label = {
              Text(
                stringResource(
                  if (uiState.isSyncing) Res.string.home_cancel_sync else Res.string.home_sync_now
                )
              )
            },
          )
          NavigationRailItem(
            selected = false,
            colors = railItemColors,
            onClick = onSignOut,
            icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
            label = { Text(stringResource(Res.string.home_sign_out)) },
          )
        }
        VerticalDivider()
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
          Box(modifier = Modifier.padding(padding)) { content() }
        }
      }
    } else {
      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
          ModalDrawerSheet(
            modifier = Modifier.width(280.dp),
            drawerContainerColor = MaterialTheme.colorScheme.primary,
          ) {
            drawerItems()
          }
        },
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
