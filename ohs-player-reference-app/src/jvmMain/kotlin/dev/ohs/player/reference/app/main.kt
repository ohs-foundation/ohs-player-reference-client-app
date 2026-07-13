package dev.ohs.player.reference.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.ohs.fhir.FhirEngine
import dev.ohs.fhir.FhirEngineConfiguration
import dev.ohs.fhir.FhirEngineProvider
import dev.ohs.player.reference.app.data.di.initKoin
import java.io.File
import org.koin.dsl.module

fun main() = application {
  val userHome = System.getProperty("user.home").orEmpty().ifBlank { "." }
  val storageDirectory = File(userHome, ".ohs-player-reference-app").absolutePath
  FhirEngineProvider.init(FhirEngineConfiguration(storageDirectory = storageDirectory))
  initKoin(module { single<FhirEngine> { FhirEngineProvider.getInstance() } })
  Window(onCloseRequest = ::exitApplication, title = "OHS Player Reference App") { App() }
}
