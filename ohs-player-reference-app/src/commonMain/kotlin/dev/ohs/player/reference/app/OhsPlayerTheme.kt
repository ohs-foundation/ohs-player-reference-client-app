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
package dev.ohs.player.reference.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OhsPrimary = Color(0xFF5A44C4)
private val OhsOnPrimary = Color.White
private val OhsPrimaryContainer = Color(0xFFE5DEFF)
private val OhsOnPrimaryContainer = Color(0xFF190261)

private val OhsSecondary = Color(0xFF2D86C4)
private val OhsOnSecondary = Color.White
private val OhsSecondaryContainer = Color(0xFFD1E4FF)
private val OhsOnSecondaryContainer = Color(0xFF001C38)

private val OhsTertiary = Color(0xFF7158C9)
private val OhsOnTertiary = Color.White
private val OhsTertiaryContainer = Color(0xFFEADDFF)
private val OhsOnTertiaryContainer = Color(0xFF230A5E)

private val OhsError = Color(0xFFB3261E)
private val OhsOnError = Color.White
private val OhsErrorContainer = Color(0xFFF9DEDC)
private val OhsOnErrorContainer = Color(0xFF601410)

private val OhsBackground = Color(0xFFFFFBFF)
private val OhsSurface = Color(0xFFFFFBFF)
private val OhsOnSurface = Color(0xFF1C1B1F)
private val OhsSurfaceVariant = Color(0xFFE5E0EC)
private val OhsOnSurfaceVariant = Color(0xFF48454E)
private val OhsOutline = Color(0xFF79767F)

private val OhsLightColorScheme =
  lightColorScheme(
    primary = OhsPrimary,
    onPrimary = OhsOnPrimary,
    primaryContainer = OhsPrimaryContainer,
    onPrimaryContainer = OhsOnPrimaryContainer,
    secondary = OhsSecondary,
    onSecondary = OhsOnSecondary,
    secondaryContainer = OhsSecondaryContainer,
    onSecondaryContainer = OhsOnSecondaryContainer,
    tertiary = OhsTertiary,
    onTertiary = OhsOnTertiary,
    tertiaryContainer = OhsTertiaryContainer,
    onTertiaryContainer = OhsOnTertiaryContainer,
    error = OhsError,
    onError = OhsOnError,
    errorContainer = OhsErrorContainer,
    onErrorContainer = OhsOnErrorContainer,
    background = OhsBackground,
    onBackground = OhsOnSurface,
    surface = OhsSurface,
    onSurface = OhsOnSurface,
    surfaceVariant = OhsSurfaceVariant,
    onSurfaceVariant = OhsOnSurfaceVariant,
    outline = OhsOutline,
  )

private val OhsDarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFC9BFFF),
    onPrimary = Color(0xFF2A1478),
    primaryContainer = Color(0xFF422F91),
    onPrimaryContainer = Color(0xFFE5DEFF),
    secondary = Color(0xFF9FCAFF),
    onSecondary = Color(0xFF00325B),
    secondaryContainer = Color(0xFF004A80),
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = Color(0xFFD3BBFF),
    onTertiary = Color(0xFF3A1D8F),
    tertiaryContainer = Color(0xFF5840B0),
    onTertiaryContainer = Color(0xFFEADDFF),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF48454E),
    onSurfaceVariant = Color(0xFFC9C5D0),
    outline = Color(0xFF938F99),
  )

@Composable
fun OhsPlayerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  val colorScheme = if (darkTheme) OhsDarkColorScheme else OhsLightColorScheme
  MaterialTheme(colorScheme = colorScheme, content = content)
}
