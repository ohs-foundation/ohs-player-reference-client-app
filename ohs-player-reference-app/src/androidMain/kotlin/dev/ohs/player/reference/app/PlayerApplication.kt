package dev.ohs.player.reference.app

import android.app.Application
import dev.ohs.fhir.FhirEngine
import dev.ohs.fhir.FhirEngineConfiguration
import dev.ohs.fhir.FhirEngineProvider
import dev.ohs.fhir.datacapture.DataCapture
import dev.ohs.player.reference.app.data.di.initKoin
import org.koin.dsl.module

class PlayerApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    FhirEngineProvider.init(FhirEngineConfiguration(), applicationContext)
    initKoin(module { single<FhirEngine> { FhirEngineProvider.getInstance(applicationContext) } })
    DataCapture.initialize(applicationContext)
  }
}
