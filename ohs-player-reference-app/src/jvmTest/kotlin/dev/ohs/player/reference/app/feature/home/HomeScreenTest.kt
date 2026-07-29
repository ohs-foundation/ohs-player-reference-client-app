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
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.ohs.fhir.engine.sync.FhirDataStore
import dev.ohs.fhir.engine.sync.SyncJobStatus
import dev.ohs.fhir.engine.sync.createDataStore
import dev.ohs.player.library.registry.LocalViewRegistry
import dev.ohs.player.reference.app.buildAppViewRegistry
import dev.ohs.player.reference.app.data.di.repositoryModule
import dev.ohs.player.reference.app.data.di.viewModelModule
import dev.ohs.player.reference.app.data.repository.FhirRepository
import dev.ohs.player.reference.app.data.repository.InMemorySampleFhirRepository
import dev.ohs.player.reference.app.data.sync.FakeSyncManager
import dev.ohs.player.reference.app.data.sync.SyncManager
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class)
class HomeScreenTest {

  private fun newFhirDataStore(): FhirDataStore {
    val path = Files.createTempFile("home-screen-test", ".preferences_pb").toString()
    return FhirDataStore(createDataStore { path })
  }

  private fun startTestKoin(syncManager: SyncManager) {
    startKoin {
      modules(
        module {
          single<FhirRepository> { InMemorySampleFhirRepository() }
          single { newFhirDataStore() }
          single<SyncManager> { syncManager }
        },
        repositoryModule,
        viewModelModule,
      )
    }
  }

  @AfterTest fun tearDown() = stopKoin()

  @Test
  fun homeScreen_defaultsToHouseholdsContentWithDrawerSectionsVisible() = runComposeUiTest {
    startTestKoin(FakeSyncManager { SyncJobStatus.Succeeded() })
    val registry = buildAppViewRegistry()
    setContent {
      CompositionLocalProvider(LocalViewRegistry provides registry) {
        MaterialTheme {
          HomeScreen(
            onGroupClick = {},
            onDataCaptureClick = {},
            onMemberClick = {},
            onAddMembers = {},
            onSignOut = {},
          )
        }
      }
    }

    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithText("No households").fetchSemanticsNodes().isNotEmpty()
    }
    assertTrue(onAllNodesWithText("Registers").fetchSemanticsNodes().isNotEmpty())
    assertTrue(onAllNodesWithText("Households").fetchSemanticsNodes().isNotEmpty())
    assertTrue(onAllNodesWithText("Sync now").fetchSemanticsNodes().isNotEmpty())
  }

  @Test
  fun tappingSyncNow_showsProgressIndicatorWhileSyncPending() = runComposeUiTest {
    val syncStarted = CompletableDeferred<Unit>()
    val releaseSyncResult = CompletableDeferred<SyncJobStatus>()
    startTestKoin(
      FakeSyncManager {
        syncStarted.complete(Unit)
        releaseSyncResult.await()
      }
    )
    val registry = buildAppViewRegistry()
    setContent {
      CompositionLocalProvider(LocalViewRegistry provides registry) {
        MaterialTheme {
          HomeScreen(
            onGroupClick = {},
            onDataCaptureClick = {},
            onMemberClick = {},
            onAddMembers = {},
            onSignOut = {},
          )
        }
      }
    }

    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithText("Sync now").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("Sync now").performClick()

    waitUntil(timeoutMillis = 5_000L) { syncStarted.isCompleted }
    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithContentDescription("Sync in progress").fetchSemanticsNodes().isNotEmpty()
    }

    releaseSyncResult.complete(SyncJobStatus.Succeeded())

    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithContentDescription("Sync in progress").fetchSemanticsNodes().isEmpty()
    }
  }

  @Test
  fun tappingCancelSync_whileSyncing_callsCancelAndShowsCancelledMessage() = runComposeUiTest {
    val syncStarted = CompletableDeferred<Unit>()
    val releaseSyncResult = CompletableDeferred<SyncJobStatus>()
    val fake = FakeSyncManager {
      syncStarted.complete(Unit)
      releaseSyncResult.await()
    }
    startTestKoin(fake)
    val registry = buildAppViewRegistry()
    setContent {
      CompositionLocalProvider(LocalViewRegistry provides registry) {
        MaterialTheme {
          HomeScreen(
            onGroupClick = {},
            onDataCaptureClick = {},
            onMemberClick = {},
            onAddMembers = {},
            onSignOut = {},
          )
        }
      }
    }

    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithText("Sync now").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("Sync now").performClick()
    waitUntil(timeoutMillis = 5_000L) { syncStarted.isCompleted }
    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithText("Cancel sync").fetchSemanticsNodes().isNotEmpty()
    }

    onNodeWithText("Cancel sync").performClick()
    waitUntil(timeoutMillis = 5_000L) { fake.cancelSyncNowCount > 0 }
    releaseSyncResult.complete(SyncJobStatus.Failed())

    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithText("Sync cancelled.").fetchSemanticsNodes().isNotEmpty()
    }
  }

  @Test
  fun tappingSyncNow_onFailure_showsSnackbarMessage() = runComposeUiTest {
    startTestKoin(FakeSyncManager { SyncJobStatus.Failed() })
    val registry = buildAppViewRegistry()
    setContent {
      CompositionLocalProvider(LocalViewRegistry provides registry) {
        MaterialTheme {
          HomeScreen(
            onGroupClick = {},
            onDataCaptureClick = {},
            onMemberClick = {},
            onAddMembers = {},
            onSignOut = {},
          )
        }
      }
    }

    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithText("Sync now").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("Sync now").performClick()

    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithText("Sync failed. Please try again.").fetchSemanticsNodes().isNotEmpty()
    }
  }
}
