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

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class InitialSyncScreenTest {

  @Test
  fun syncingState_showsBlockingProgressText() = runComposeUiTest {
    setContent {
      MaterialTheme {
        InitialSyncScreen(state = InitialSyncGateState.Syncing, onRetry = {}, onContinueAnyway = {})
      }
    }

    assertTrue(
      onAllNodesWithText("Setting up your data", substring = true)
        .fetchSemanticsNodes()
        .isNotEmpty()
    )
  }

  @Test
  fun failedState_tappingRetry_invokesCallback() = runComposeUiTest {
    var retried = false
    setContent {
      MaterialTheme {
        InitialSyncScreen(
          state = InitialSyncGateState.Failed,
          onRetry = { retried = true },
          onContinueAnyway = {},
        )
      }
    }

    onNodeWithText("Retry", ignoreCase = true).performClick()

    assertTrue(retried)
  }

  @Test
  fun failedState_tappingContinueAnyway_invokesCallback() = runComposeUiTest {
    var continued = false
    setContent {
      MaterialTheme {
        InitialSyncScreen(
          state = InitialSyncGateState.Failed,
          onRetry = {},
          onContinueAnyway = { continued = true },
        )
      }
    }

    onNodeWithText("Continue without syncing", ignoreCase = true).performClick()

    assertTrue(continued)
  }
}
