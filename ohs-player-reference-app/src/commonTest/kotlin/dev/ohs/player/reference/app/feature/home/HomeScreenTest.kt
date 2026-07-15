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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.ohs.player.library.registry.LocalViewRegistry
import dev.ohs.player.reference.app.buildAppViewRegistry
import dev.ohs.player.reference.app.data.di.repositoryModule
import dev.ohs.player.reference.app.data.di.viewModelModule
import dev.ohs.player.reference.app.data.repository.FhirRepository
import dev.ohs.player.reference.app.data.repository.InMemorySampleFhirRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class)
class HomeScreenTest {

  @BeforeTest
  fun setUp() {
    startKoin {
      modules(
        module { single<FhirRepository> { InMemorySampleFhirRepository() } },
        repositoryModule,
        viewModelModule,
      )
    }
  }

  @AfterTest fun tearDown() = stopKoin()

  @Test
  fun homeScreen_defaultsToHouseholdsContentWithDrawerSectionsVisible() = runComposeUiTest {
    val registry = buildAppViewRegistry()
    setContent {
      CompositionLocalProvider(LocalViewRegistry provides registry) {
        MaterialTheme {
          HomeScreen(
            onGroupClick = {},
            onDataCaptureClick = {},
            onSyncNowClick = {},
            lastSyncedAt = null,
          )
        }
      }
    }

    // GroupListScreen's own empty state, proving it's the active content by default.
    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithText("No households").fetchSemanticsNodes().isNotEmpty()
    }
    assertTrue(onAllNodesWithText("Registers").fetchSemanticsNodes().isNotEmpty())
    assertTrue(onAllNodesWithText("Households").fetchSemanticsNodes().isNotEmpty())
    assertTrue(onAllNodesWithText("Sync now").fetchSemanticsNodes().isNotEmpty())
  }

  @Test
  fun homeScreen_tappingSyncNow_invokesCallback() = runComposeUiTest {
    val registry = buildAppViewRegistry()
    var syncClicked = false
    setContent {
      CompositionLocalProvider(LocalViewRegistry provides registry) {
        MaterialTheme {
          HomeScreen(
            onGroupClick = {},
            onDataCaptureClick = {},
            onSyncNowClick = { syncClicked = true },
            lastSyncedAt = null,
          )
        }
      }
    }

    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithText("Sync now").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("Sync now").performClick()
    assertTrue(syncClicked)
  }

  @Test
  fun homeScreen_lastSyncedAtNull_hidesLastSyncedText() = runComposeUiTest {
    val registry = buildAppViewRegistry()
    setContent {
      CompositionLocalProvider(LocalViewRegistry provides registry) {
        MaterialTheme {
          HomeScreen(
            onGroupClick = {},
            onDataCaptureClick = {},
            onSyncNowClick = {},
            lastSyncedAt = null,
          )
        }
      }
    }

    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithText("Sync now").fetchSemanticsNodes().isNotEmpty()
    }
    assertEquals(0, onAllNodesWithText("Last synced", substring = true).fetchSemanticsNodes().size)
  }

  @Test
  fun homeScreen_lastSyncedAtProvided_showsLastSyncedText() = runComposeUiTest {
    val registry = buildAppViewRegistry()
    setContent {
      CompositionLocalProvider(LocalViewRegistry provides registry) {
        MaterialTheme {
          HomeScreen(
            onGroupClick = {},
            onDataCaptureClick = {},
            onSyncNowClick = {},
            lastSyncedAt = "2026-07-15 10:00",
          )
        }
      }
    }

    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithText("Last synced: 2026-07-15 10:00").fetchSemanticsNodes().isNotEmpty()
    }
  }
}
