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

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeWidthBreakpointTest {

  @Test
  fun isHomeDrawerExpandedWidth_belowBreakpoint_isFalse() {
    assertFalse(isHomeDrawerExpandedWidth(600.dp))
    assertFalse(isHomeDrawerExpandedWidth(839.dp))
  }

  @Test
  fun isHomeDrawerExpandedWidth_atOrAboveBreakpoint_isTrue() {
    assertTrue(isHomeDrawerExpandedWidth(840.dp))
    assertTrue(isHomeDrawerExpandedWidth(1200.dp))
  }
}
