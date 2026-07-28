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
      is InitialSyncGateState.Failed -> FailedContent(state.message, onRetry, onContinueAnyway)
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
      text = "Setting up your data…",
      style = MaterialTheme.typography.titleMedium,
      textAlign = TextAlign.Center,
    )
    Text(
      text = "This may take a moment the first time.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun FailedContent(message: String, onRetry: () -> Unit, onContinueAnyway: () -> Unit) {
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
      text = "Couldn't sync your data",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.error,
      textAlign = TextAlign.Center,
    )
    Text(text = message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Retry") }
    TextButton(onClick = onContinueAnyway, modifier = Modifier.fillMaxWidth()) {
      Text("Continue without syncing")
    }
  }
}
