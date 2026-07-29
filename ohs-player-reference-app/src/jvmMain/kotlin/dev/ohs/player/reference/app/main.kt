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

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.ohs.fhir.engine.FhirEngine
import dev.ohs.fhir.engine.FhirEngineConfiguration
import dev.ohs.fhir.engine.FhirEngineProvider
import dev.ohs.fhir.engine.NetworkConfiguration
import dev.ohs.fhir.engine.ServerConfiguration
import dev.ohs.fhir.engine.sync.remote.HttpLogger
import dev.ohs.player.reference.app.auth.FhirBearerAuthenticator
import dev.ohs.player.reference.app.auth.GeneratedAuthConfig
import dev.ohs.player.reference.app.data.di.initKoin
import dev.ohs.player.reference.app.data.sync.ForegroundPeriodicSyncUseCase
import dev.ohs.player.reference.app.data.sync.ForegroundSyncNowUseCase
import dev.ohs.player.reference.app.data.sync.PeriodicSyncUseCase
import dev.ohs.player.reference.app.data.sync.SYNC_TIMEOUT_DURATION
import dev.ohs.player.reference.app.data.sync.SyncNowUseCase
import java.io.File
import org.koin.dsl.module

fun main() = application {
  val userHome = System.getProperty("user.home").orEmpty().ifBlank { "." }
  val storageDirectory = File(userHome, ".ohs-player-reference-app").absolutePath
  FhirEngineProvider.init(
    FhirEngineConfiguration(
      storageDirectory = storageDirectory,
      serverConfiguration =
        ServerConfiguration(
          baseUrl = GeneratedAuthConfig.FHIR_BASE_URL,
          networkConfiguration =
            NetworkConfiguration(
              connectionTimeOut = SYNC_TIMEOUT_DURATION,
              readTimeOut = SYNC_TIMEOUT_DURATION,
              writeTimeOut = SYNC_TIMEOUT_DURATION,
            ),
          httpLogger = HttpLogger(level = HttpLogger.Level.HEADERS),
          authenticator = FhirBearerAuthenticator,
        ),
    )
  )
  initKoin(
    module {
      single<FhirEngine> { FhirEngineProvider.getInstance() }
      single<SyncNowUseCase> { ForegroundSyncNowUseCase() }
      single<PeriodicSyncUseCase> { ForegroundPeriodicSyncUseCase() }
    }
  )
  Window(onCloseRequest = ::exitApplication, title = "OHS Player Reference App") { App() }
}
