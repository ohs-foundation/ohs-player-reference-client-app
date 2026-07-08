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
package dev.ohs.player.reference.app.feature.questionnaire

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.ohs.fhir.datacapture.Questionnaire
import dev.ohs.fhir.datacapture.QuestionnaireConfig
import dev.ohs.fhir.datacapture.QuestionnaireItemViewFactoryMatcher
import dev.ohs.fhir.datacapture.QuestionnaireItemViewFactoryMatchersProvider
import dev.ohs.player.reference.app.data.AppDependencies
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadQuestionnaireJson(path: String): String =
  Res.readBytes(path).decodeToString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireHostScreen(
  questionnaireId: String,
  patientId: String? = null,
  groupId: String? = null,
  onBack: () -> Unit,
) {
  val questionnaireDefinition =
    remember(questionnaireId) { QuestionnaireRegistry.find(questionnaireId) }
  val launchContext =
    remember(patientId, groupId) {
      QuestionnaireLaunchContext(patientId = patientId, groupId = groupId)
    }
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
  var questionnaireJson by
    remember(questionnaireId, patientId, groupId) { mutableStateOf<String?>(null) }
  var launchContextMap by
    remember(questionnaireId, patientId, groupId) {
      mutableStateOf<Map<String, String>>(emptyMap())
    }
  var loadError by remember(questionnaireId) { mutableStateOf<String?>(null) }
  var submitError by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }
  val latestOnBack by rememberUpdatedState(onBack)

  LaunchedEffect(successMessage) {
    if (successMessage == null) return@LaunchedEffect
    delay(2_000.milliseconds)
    successMessage = null
    latestOnBack()
  }

  LaunchedEffect(questionnaireDefinition?.id, patientId, groupId) {
    val definition =
      questionnaireDefinition
        ?: run {
          loadError = "Questionnaire \"$questionnaireId\" was not found."
          return@LaunchedEffect
        }
    runCatching {
        val sourceQuestionnaireJson = loadQuestionnaireJson(definition.questionnairePath)
        questionnaireJson =
          definition.prepareQuestionnaireJson(
            QuestionnairePreparationContext(
              sourceQuestionnaireJson = sourceQuestionnaireJson,
              launchContext = launchContext,
              fhirJson = fhirJson,
            )
          )
        launchContextMap = definition.buildLaunchContextMap(launchContext)
      }
      .onSuccess { loadError = null }
      .onFailure { throwable ->
        questionnaireJson = null
        loadError = throwable.message ?: "The questionnaire could not be loaded."
      }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(questionnaireDefinition?.title ?: "Questionnaire") },
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
      successMessage?.let { message -> SubmissionBanner(message = message, isSuccess = true) }
      submitError?.let { message -> SubmissionBanner(message = message, isSuccess = false) }

      Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        when {
          loadError != null -> {
            Text(
              text = loadError ?: "Something went wrong.",
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(16.dp),
            )
          }

          questionnaireDefinition == null || questionnaireJson == null -> {
            CircularProgressIndicator()
          }

          else -> {
            Questionnaire(
              questionnaireJson = questionnaireJson.orEmpty(),
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
                  submitError = null
                  successMessage = null
                  runCatching {
                      questionnaireDefinition.submit(
                        QuestionnaireSubmissionContext(
                          questionnaireJson = questionnaireJson.orEmpty(),
                          response = getResponse(),
                          launchContext = launchContext,
                          repository = AppDependencies.fhirRepository,
                          fhirJson = fhirJson,
                        )
                      )
                    }
                    .onSuccess { result ->
                      println("========== Extracted Bundle ==========")
                      println(result.bundleJson)
                      successMessage = result.successMessage
                    }
                    .onFailure { throwable ->
                      submitError =
                        throwable.message
                          ?: "Bundle extraction could not be completed. Please try again."
                    }
                }
              },
              matchersProvider = viewItemMatchersProvider,
              onCancel = onBack,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SubmissionBanner(message: String, isSuccess: Boolean) {
  Surface(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    shape = RoundedCornerShape(8.dp),
    color = if (isSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
    tonalElevation = 2.dp,
  ) {
    Text(
      text = message,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      color = Color.White,
      style = MaterialTheme.typography.bodySmall,
    )
  }
}
