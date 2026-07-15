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

import android.app.Application
import dev.ohs.fhir.FhirEngine
import dev.ohs.fhir.FhirEngineConfiguration
import dev.ohs.fhir.FhirEngineProvider
import dev.ohs.fhir.datacapture.DataCapture
import dev.ohs.player.reference.app.data.di.initKoin
import org.koin.dsl.module

class OhsPlayerApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    FhirEngineProvider.init(FhirEngineConfiguration(), applicationContext)
    initKoin(module { single<FhirEngine> { FhirEngineProvider.getInstance(applicationContext) } })
    DataCapture.initialize(applicationContext)
  }
}
