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
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.Resource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdRegistrationScreen(onBack: () -> Unit) {
  val launchContextMap = remember {
    mapOf("patient" to "{\"resourceType\":\"Patient\",\"id\":\"P1\"}")
  }
  var questionnaireJson by remember { mutableStateOf<String?>(null) }
  val viewItemMatchersProvider = remember {
    object : QuestionnaireItemViewFactoryMatchersProvider {
      override fun get(): List<QuestionnaireItemViewFactoryMatcher> = listOf()
    }
  }
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(Unit) { questionnaireJson = loadQuestionnaire() }
  val jsonR4 = Json {
    prettyPrint = true
    explicitNulls = false
    encodeDefaults = false
    ignoreUnknownKeys = true
  }
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
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
          ),
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
                val response = getResponse()

                val questionnaire = jsonR4.decodeFromString<Questionnaire>(json)
                val bundleResponse = TemplateExtractionEngine.extract(questionnaire, response)
                bundleResponse.entry.mapIndexed { index, entry ->
                  println("========== Resource ${index + 1} ==========")
                  entry.resource?.let { resource ->
                    println(jsonR4.encodeToString(Resource.serializer(), resource))
                  } ?: println("Resource is null")
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

@OptIn(ExperimentalResourceApi::class)
suspend fun loadQuestionnaire(): String {
  return Res.readBytes("files/configs/Questionnaire-HouseholdRegistration.json").decodeToString()
}
