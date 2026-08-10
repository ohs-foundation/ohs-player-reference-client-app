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
package dev.ohs.player.reference.app.feature.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import player_reference.reference_app.generated.resources.Res
import player_reference.reference_app.generated.resources.initial_sync_continue
import player_reference.reference_app.generated.resources.initial_sync_failed_body
import player_reference.reference_app.generated.resources.initial_sync_failed_title
import player_reference.reference_app.generated.resources.initial_sync_retry
import player_reference.reference_app.generated.resources.initial_sync_subtitle
import player_reference.reference_app.generated.resources.initial_sync_title

/**
 * Full-screen blocking gate shown between login and Home: a progress state while checking/syncing,
 * and a failure state offering Retry or Continue without syncing. Never shown for [Passed] — the
 * caller ([dev.ohs.player.reference.app.App]) switches to the real content on that state instead.
 */
@Composable
fun InitialSyncScreen(
  state: InitialSyncGateState,
  onRetry: () -> Unit,
  onContinueAnyway: () -> Unit,
) {
  Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
    when (state) {
      InitialSyncGateState.Checking,
      InitialSyncGateState.Syncing -> SyncingContent()
      InitialSyncGateState.Failed -> FailedContent(onRetry, onContinueAnyway)
      InitialSyncGateState.Passed -> Unit
    }
  }
}

@Composable
private fun SyncingContent() {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    CircularProgressIndicator()
    Text(
      text = stringResource(Res.string.initial_sync_title),
      style = MaterialTheme.typography.titleMedium,
      textAlign = TextAlign.Center,
    )
    Text(
      text = stringResource(Res.string.initial_sync_subtitle),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun FailedContent(onRetry: () -> Unit, onContinueAnyway: () -> Unit) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Icon(
      imageVector = Icons.Filled.Warning,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.error,
      modifier = Modifier.size(48.dp),
    )
    Text(
      text = stringResource(Res.string.initial_sync_failed_title),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.error,
      textAlign = TextAlign.Center,
    )
    Text(
      text = stringResource(Res.string.initial_sync_failed_body),
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
    )
    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(Res.string.initial_sync_retry))
    }
    TextButton(onClick = onContinueAnyway, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(Res.string.initial_sync_continue))
    }
  }
}
