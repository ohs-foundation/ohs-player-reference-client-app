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

@Composable
fun App() {
  val registry = remember { buildAppViewRegistry() }

  CompositionLocalProvider(LocalViewRegistry provides registry) {
    OhsPlayerTheme {
      val navController = rememberNavController()
      NavHost(navController = navController, startDestination = "groupList") {

        // Screen 1: Household list
        composable("groupList") {
          GroupListScreen(
            onGroupClick = { id -> navController.navigate("groupProfile/$id") },
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
          route = "questionnaireHost/{questionnaireId}?patientId={patientId}&groupId={groupId}",
          arguments =
            listOf(
              navArgument("questionnaireId") { type = NavType.StringType },
              navArgument("patientId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
              },
              navArgument("groupId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
              },
            ),
        ) { back ->
          val questionnaireId =
            back.arguments?.read { getStringOrNull("questionnaireId") }.orEmpty()
          val patientId = back.arguments?.read { getStringOrNull("patientId") }
          val groupId = back.arguments?.read { getStringOrNull("groupId") }
          QuestionnaireHostScreen(
            questionnaireId = questionnaireId,
            patientId = patientId,
            groupId = groupId,
            onBack = { navController.popBackStack() },
          )
        }

        // Screen 2: Household profile (head + members)
        composable(
          route = "groupProfile/{groupId}",
          arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
        ) { back ->
          val groupId = back.arguments?.read { getStringOrNull("groupId") }.orEmpty()
          GroupProfileScreen(
            groupId = groupId,
            onBack = { navController.popBackStack() },
            onMemberClick = { id -> navController.navigate("patientProfile/$id") },
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
          route = "patientProfile/{patientId}",
          arguments = listOf(navArgument("patientId") { type = NavType.StringType }),
        ) { back ->
          val patientId = back.arguments?.read { getStringOrNull("patientId") }.orEmpty()
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
  append("questionnaireHost/$questionnaireId")
  val queryParameters =
    listOfNotNull(patientId?.let { "patientId=$it" }, groupId?.let { "groupId=$it" })
  if (queryParameters.isNotEmpty()) {
    append("?")
    append(queryParameters.joinToString("&"))
  }
}

@OptIn(ExperimentalUuidApi::class) fun generateId(): String = Uuid.random().toString()
