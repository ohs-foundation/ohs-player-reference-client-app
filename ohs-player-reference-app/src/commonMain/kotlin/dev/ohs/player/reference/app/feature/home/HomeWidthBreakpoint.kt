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

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Material 3's "expanded" window size class breakpoint. */
internal val HOME_EXPANDED_WIDTH_BREAKPOINT: Dp = 840.dp

/**
 * True once the available width reaches [HOME_EXPANDED_WIDTH_BREAKPOINT], where [HomeScreen]
 * switches from a hamburger-triggered modal drawer to an always-visible permanent drawer.
 */
internal fun isHomeDrawerExpandedWidth(width: Dp): Boolean = width >= HOME_EXPANDED_WIDTH_BREAKPOINT
