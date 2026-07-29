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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.ohs.fhir.datacapture.Questionnaire
import dev.ohs.fhir.datacapture.QuestionnaireConfig
import dev.ohs.fhir.datacapture.QuestionnaireItemViewFactoryMatcher
import dev.ohs.fhir.datacapture.QuestionnaireItemViewFactoryMatchersProvider
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.Res
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.questionnaire_back
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.questionnaire_retry
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.questionnaire_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireHostScreen(
  questionnaireId: String,
  patientId: String? = null,
  groupId: String? = null,
  onBack: () -> Unit,
) {
  val launchContext =
    remember(patientId, groupId) {
      QuestionnaireLaunchContext(patientId = patientId, groupId = groupId)
    }
  val viewItemMatchersProvider = remember {
    object : QuestionnaireItemViewFactoryMatchersProvider {
      override fun get(): List<QuestionnaireItemViewFactoryMatcher> = listOf()
    }
  }

  val viewModel =
    koinViewModel<QuestionnaireHostViewModel>(key = "$questionnaireId:$patientId:$groupId") {
      parametersOf(questionnaireId, launchContext)
    }
  val uiState by viewModel.uiState.collectAsState()
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(uiState) {
    if (uiState !is QuestionnaireHostUiState.Submitted) return@LaunchedEffect
    delay(2_000.milliseconds)
    onBack()
  }
  val title =
    when (val state = uiState) {
      is QuestionnaireHostUiState.Ready -> state.title
      is QuestionnaireHostUiState.Submitting -> state.title
      else -> null
    }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(title ?: stringResource(Res.string.questionnaire_title)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(Res.string.questionnaire_back),
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
    Box(
      modifier = Modifier.fillMaxSize().padding(padding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        when (val state = uiState) {
          is QuestionnaireHostUiState.Submitted ->
            SubmissionBanner(message = state.result.successMessage, isSuccess = true)

          is QuestionnaireHostUiState.Error -> Unit // rendered below, inline with a dismiss action
          else -> Unit
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
          when (val state = uiState) {
            is QuestionnaireHostUiState.Loading -> CircularProgressIndicator()

            is QuestionnaireHostUiState.Error -> {
              Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = state.message,
                  color = MaterialTheme.colorScheme.error,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(16.dp),
                )
                TextButton(onClick = viewModel::load) {
                  Text(stringResource(Res.string.questionnaire_retry))
                }
              }
            }

            is QuestionnaireHostUiState.Ready,
            is QuestionnaireHostUiState.Submitting -> {
              val questionnaireJson =
                when (state) {
                  is QuestionnaireHostUiState.Ready -> state.questionnaireJson
                  is QuestionnaireHostUiState.Submitting -> state.questionnaireJson
                  else -> ""
                }
              Questionnaire(
                questionnaireJson = questionnaireJson,
                questionnaireLaunchContextMap = emptyMap(),
                config =
                  QuestionnaireConfig(
                    showReviewPage = true,
                    showReviewPageFirst = false,
                    isReadOnly = false,
                    showCancelButton = false,
                  ),
                onSubmit = { getResponse ->
                  coroutineScope.launch { viewModel.onSubmit(getResponse()) }
                },
                matchersProvider = viewItemMatchersProvider,
                onCancel = {},
              )
            }

            is QuestionnaireHostUiState.Submitted -> Unit
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
