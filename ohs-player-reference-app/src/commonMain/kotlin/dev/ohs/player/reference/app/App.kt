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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import dev.ohs.player.library.registry.LocalViewRegistry
import dev.ohs.player.reference.app.feature.group.list.GroupListScreen
import dev.ohs.player.reference.app.feature.group.profile.GroupProfileScreen
import dev.ohs.player.reference.app.feature.patient.profile.PatientProfileScreen
import dev.ohs.player.reference.app.feature.questionnaire.QuestionnaireHostScreen
import dev.ohs.player.reference.app.feature.questionnaire.QuestionnaireRegistry
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val GROUP_LIST_ROUTE = "groupList"
private const val GROUP_PROFILE_ROUTE = "groupProfile"
private const val PATIENT_PROFILE_ROUTE = "patientProfile"
private const val QUESTIONNAIRE_HOST_ROUTE = "questionnaireHost"
private const val GROUP_ID_ARG = "groupId"
private const val PATIENT_ID_ARG = "patientId"
private const val QUESTIONNAIRE_ID_ARG = "questionnaireId"

@Composable
fun App() {
  val registry = remember { buildAppViewRegistry() }

  CompositionLocalProvider(LocalViewRegistry provides registry) {
    OhsPlayerTheme {
      val navController = rememberNavController()
      NavHost(navController = navController, startDestination = GROUP_LIST_ROUTE) {

        // Screen 1: Household list
        composable(GROUP_LIST_ROUTE) {
          GroupListScreen(
            onGroupClick = { id -> navController.navigate("$GROUP_PROFILE_ROUTE/$id") },
            onDataCaptureClick = {
              navController.navigate(
                questionnaireHostRoute(
                  questionnaireId = QuestionnaireRegistry.HOUSEHOLD_REGISTRATION_ID
                )
              )
            },
          )
        }

        composable(
          route =
            "$QUESTIONNAIRE_HOST_ROUTE/{$QUESTIONNAIRE_ID_ARG}?$PATIENT_ID_ARG={$PATIENT_ID_ARG}&$GROUP_ID_ARG={$GROUP_ID_ARG}",
          arguments =
            listOf(
              navArgument(QUESTIONNAIRE_ID_ARG) { type = NavType.StringType },
              navArgument(PATIENT_ID_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
              },
              navArgument(GROUP_ID_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
              },
            ),
        ) { back ->
          val questionnaireId =
            back.arguments?.read { getStringOrNull(QUESTIONNAIRE_ID_ARG) }.orEmpty()
          val patientId = back.arguments?.read { getStringOrNull(PATIENT_ID_ARG) }
          val groupId = back.arguments?.read { getStringOrNull(GROUP_ID_ARG) }
          QuestionnaireHostScreen(
            questionnaireId = questionnaireId,
            patientId = patientId,
            groupId = groupId,
            onBack = { navController.popBackStack() },
          )
        }

        // Screen 2: Household profile (head + members)
        composable(
          route = "$GROUP_PROFILE_ROUTE/{$GROUP_ID_ARG}",
          arguments = listOf(navArgument(GROUP_ID_ARG) { type = NavType.StringType }),
        ) { back ->
          val groupId = back.arguments?.read { getStringOrNull(GROUP_ID_ARG) }.orEmpty()
          GroupProfileScreen(
            groupId = groupId,
            onBack = { navController.popBackStack() },
            onMemberClick = { id -> navController.navigate("$PATIENT_PROFILE_ROUTE/$id") },
            onAddMembers = {
              navController.navigate(
                questionnaireHostRoute(
                  questionnaireId = QuestionnaireRegistry.HOUSEHOLD_MEMBERS_ID,
                  groupId = groupId,
                )
              )
            },
          )
        }

        // Screen 3: Patient IPS summary
        composable(
          route = "$PATIENT_PROFILE_ROUTE/{$PATIENT_ID_ARG}",
          arguments = listOf(navArgument(PATIENT_ID_ARG) { type = NavType.StringType }),
        ) { back ->
          val patientId = back.arguments?.read { getStringOrNull(PATIENT_ID_ARG) }.orEmpty()
          PatientProfileScreen(
            patientId = patientId,
            onBack = { navController.popBackStack() },
            onAddClinicalData = {
              navController.navigate(
                questionnaireHostRoute(
                  questionnaireId = QuestionnaireRegistry.PATIENT_CLINICAL_DATA_ID,
                  patientId = patientId,
                )
              )
            },
          )
        }
      }
    }
  }
}

private fun questionnaireHostRoute(
  questionnaireId: String,
  patientId: String? = null,
  groupId: String? = null,
): String = buildString {
  append("$QUESTIONNAIRE_HOST_ROUTE/$questionnaireId")
  val queryParameters =
    listOfNotNull(patientId?.let { "$PATIENT_ID_ARG=$it" }, groupId?.let { "$GROUP_ID_ARG=$it" })
  if (queryParameters.isNotEmpty()) {
    append("?")
    append(queryParameters.joinToString("&"))
  }
}

@OptIn(ExperimentalUuidApi::class)
fun generateId(): String {
  // Generates a cryptographically secure random UUID (v4)
  val uuid = Uuid.random()
  return uuid.toString()
}
