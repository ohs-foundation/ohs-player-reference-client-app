package dev.ohs.player.reference.app

import androidx.compose.ui.window.ComposeUIViewController
import dev.ohs.fhir.FhirEngine
import dev.ohs.fhir.FhirEngineConfiguration
import dev.ohs.fhir.FhirEngineProvider
import dev.ohs.player.reference.app.data.di.initKoin
import org.koin.dsl.module

fun MainViewController() = run {
  FhirEngineProvider.init(FhirEngineConfiguration())
  initKoin(module { single<FhirEngine> { FhirEngineProvider.getInstance() } })
  ComposeUIViewController { App() }
}
