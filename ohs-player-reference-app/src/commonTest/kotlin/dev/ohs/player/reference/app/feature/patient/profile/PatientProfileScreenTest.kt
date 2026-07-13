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
package dev.ohs.player.reference.app.feature.patient.profile

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import dev.ohs.player.library.registry.LocalViewRegistry
import dev.ohs.player.reference.app.buildAppViewRegistry
import dev.ohs.player.reference.app.data.di.repositoryModule
import dev.ohs.player.reference.app.data.repository.InMemorySampleFhirRepository
import dev.ohs.player.reference.app.data.repository.SamplePatientFixture
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class)
class PatientProfileScreenTest {

  @BeforeTest
  fun setUp() = runTest {
    stopKoin()
    val repository = InMemorySampleFhirRepository()
    SamplePatientFixture.resources.forEach { repository.upsert(it) }
    startKoin {
      modules(
        module { single { repository } },
        repositoryModule,
      )
    }
  }

  @Test
  fun knownPatient_rendersNameAndClinicalSections() = runComposeUiTest {
    val registry = buildAppViewRegistry()
    setContent {
      CompositionLocalProvider(LocalViewRegistry provides registry) {
        MaterialTheme {
          PatientProfileScreen(patientId = "p1", onBack = {}, onAddClinicalData = {})
        }
      }
    }

    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithText("Amina Diallo").fetchSemanticsNodes().isNotEmpty()
    }
    val scrollable = onNode(hasScrollAction())
    listOf("Amina Diallo", "Allergies", "Medications", "Conditions", "Immunizations").forEach { text
      ->
      scrollable.performScrollToNode(hasText(text))
      assertTrue(
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty(),
        "Expected to find '$text' after scrolling the patient profile",
      )
    }
  }
}
