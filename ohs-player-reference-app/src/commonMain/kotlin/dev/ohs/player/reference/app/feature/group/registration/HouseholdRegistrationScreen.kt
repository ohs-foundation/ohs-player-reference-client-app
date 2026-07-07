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
package dev.ohs.player.reference.app.feature.group.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ohs.fhir.datacapture.Questionnaire
import dev.ohs.fhir.datacapture.QuestionnaireConfig
import dev.ohs.fhir.datacapture.QuestionnaireItemViewFactoryMatcher
import dev.ohs.fhir.datacapture.QuestionnaireItemViewFactoryMatchersProvider
import dev.ohs.fhir.datacapture.extraction.template.TemplateExtractionEngine
import dev.ohs.fhir.model.r4.Bundle
import dev.ohs.fhir.model.r4.Questionnaire as QuestionnaireR4
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

private const val HOUSEHOLD_REGISTRATION_QUESTIONNAIRE_PATH =
  "files/configs/Questionnaire-HouseholdRegistration.json"

@OptIn(ExperimentalResourceApi::class)
suspend fun householdRegistrationQuestionnaireJson(): String =
  Res.readBytes(HOUSEHOLD_REGISTRATION_QUESTIONNAIRE_PATH).decodeToString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdRegistrationScreen(onBack: () -> Unit) {
  val launchContextMap = remember {
    mapOf("patient" to "{\"resourceType\":\"Patient\",\"id\":\"P1\"}")
  }
  var questionnaireJson by remember { mutableStateOf<String?>(null) }
  var extractedBundleMessage by remember { mutableStateOf<String?>(null) }
  var submitError by remember { mutableStateOf<String?>(null) }
  val viewItemMatchersProvider = remember {
    object : QuestionnaireItemViewFactoryMatchersProvider {
      override fun get(): List<QuestionnaireItemViewFactoryMatcher> = listOf()
    }
  }
  val coroutineScope = rememberCoroutineScope()
  val fhirJson = remember {
    Json {
      prettyPrint = true
      explicitNulls = false
      encodeDefaults = false
      ignoreUnknownKeys = true
    }
  }

  LaunchedEffect(Unit) { questionnaireJson = householdRegistrationQuestionnaireJson() }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Household registration") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onPrimary,
            )
          }
        },
        actions = {
          submitError?.let { TextButton(onClick = { submitError = null }) { Text("Dismiss") } }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
          ),
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).padding(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      extractedBundleMessage?.let { message ->
        Text(
          text = message,
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.bodySmall,
        )
      }

      submitError?.let { message ->
        Text(
          text = message,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
      }

      Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        questionnaireJson?.let { json ->
          Questionnaire(
            questionnaireJson = json,
            questionnaireLaunchContextMap = launchContextMap,
            config =
              QuestionnaireConfig(
                showReviewPage = true,
                showReviewPageFirst = false,
                isReadOnly = false,
                showCancelButton = false,
              ),
            onSubmit = { getResponse ->
              coroutineScope.launch {
                extractedBundleMessage = null
                submitError = null
                runCatching {
                    val questionnaire =
                      fhirJson.decodeFromString(QuestionnaireR4.serializer(), json)
                    val bundle = TemplateExtractionEngine.extract(questionnaire, getResponse())
                    fhirJson.encodeToString(Bundle.serializer(), bundle)
                  }
                  .onSuccess { bundleJson ->
                    println("========== Extracted Bundle ==========")
                    println(bundleJson)
                    extractedBundleMessage = "Bundle extracted successfully. Check logs for JSON."
                  }
                  .onFailure { throwable ->
                    submitError =
                      throwable.message
                        ?: "Bundle extraction could not be completed. Please try again."
                  }
              }
            },
            matchersProvider = viewItemMatchersProvider,
            onCancel = {},
          )
        }
      }
    }
  }
}
