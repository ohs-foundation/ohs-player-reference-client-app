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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.Res
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.home_destination_households
import org.jetbrains.compose.resources.StringResource

/**
 * A top-level destination reachable from [HomeScreen]'s navigation drawer. `Households` is the only
 * entry today; adding a second destination later is a new enum entry, not a rewrite.
 */
enum class HomeDestination(val label: StringResource, val icon: ImageVector) {
  Households(label = Res.string.home_destination_households, icon = Icons.Filled.Home)
}
