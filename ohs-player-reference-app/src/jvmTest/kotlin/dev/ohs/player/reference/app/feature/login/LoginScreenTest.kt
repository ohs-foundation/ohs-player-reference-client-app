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
package dev.ohs.player.reference.app.feature.login

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LoginScreenTest {

  @Test
  fun tappingSignIn_invokesCallback() = runComposeUiTest {
    var clicked = false
    setContent {
      MaterialTheme {
        LoginScreen(signingIn = false, error = null, onSignIn = { clicked = true }, onErrorDismiss = {})
      }
    }

    onNodeWithText("Sign in", ignoreCase = true).performClick()

    assertTrue(clicked)
  }

  @Test
  fun error_showsDialogWithMessage_andDismissClearsIt() = runComposeUiTest {
    var dismissed = false
    setContent {
      MaterialTheme {
        LoginScreen(
          signingIn = false,
          error = "Sign-in failed: invalid_grant",
          onSignIn = {},
          onErrorDismiss = { dismissed = true },
        )
      }
    }

    assertTrue(onAllNodesWithText("Sign-in failed: invalid_grant").fetchSemanticsNodes().isNotEmpty())
    onNodeWithText("Dismiss", ignoreCase = true).performClick()

    assertTrue(dismissed)
  }
}
