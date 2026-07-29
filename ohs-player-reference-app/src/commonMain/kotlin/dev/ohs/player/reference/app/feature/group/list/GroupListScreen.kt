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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ohs.player.generated.state.GroupListState
import dev.ohs.player.generated.viewtype.ViewTypeCS
import dev.ohs.player.library.layout.VerticalListRenderer
import dev.ohs.player.library.scaffold.ListScaffold
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.Res
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.group_list_empty
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.group_list_register_household
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(onGroupClick: (String) -> Unit, onDataCaptureClick: () -> Unit) {
  val viewModel: GroupListViewModel = koinViewModel()
  val groups by viewModel.groups.collectAsStateWithLifecycle()

  if (groups == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
    return
  }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onDataCaptureClick,
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text(stringResource(Res.string.group_list_register_household)) },
      )
    },
  ) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
      ListScaffold<GroupListState>(
        items = groups!!,
        onItemClick = { onGroupClick(it.groupId ?: "") },
        key = { it.groupId ?: it.hashCode().toString() },
      ) {
        component(ViewTypeCS.GroupCard)
        layout(VerticalListRenderer.VIEW_TYPE)
        emptyState { Text(stringResource(Res.string.group_list_empty)) }
      }
    }
  }
}
