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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import dev.ohs.player.library.registry.LocalViewRegistry
import dev.ohs.player.reference.app.auth.AuthState
import dev.ohs.player.reference.app.auth.AuthViewModel
import dev.ohs.player.reference.app.auth.rememberAuthorizationLauncher
import dev.ohs.player.reference.app.feature.group.profile.GroupProfileScreen
import dev.ohs.player.reference.app.feature.home.HomeScreen
import dev.ohs.player.reference.app.feature.login.LoginScreen
import dev.ohs.player.reference.app.feature.patient.profile.PatientProfileScreen
import dev.ohs.player.reference.app.feature.questionnaire.QuestionnaireHostScreen
import dev.ohs.player.reference.app.feature.questionnaire.QuestionnaireIds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
  val registry = remember { buildAppViewRegistry() }

  CompositionLocalProvider(LocalViewRegistry provides registry) {
    OhsPlayerTheme {
      val authViewModel: AuthViewModel = koinViewModel()
      val launcher = rememberAuthorizationLauncher()
      val authState by authViewModel.state.collectAsStateWithLifecycle()
      val signingIn by authViewModel.signingIn.collectAsStateWithLifecycle()
      val authError by authViewModel.error.collectAsStateWithLifecycle()

      LaunchedEffect(launcher) { authViewModel.bootstrap(launcher) }

      when (authState) {
        is AuthState.Loading ->
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        is AuthState.Unauthenticated ->
          LoginScreen(
            signingIn = signingIn,
            error = authError,
            onSignIn = { authViewModel.login(launcher) },
            onErrorDismiss = { authViewModel.clearError() },
          )
        is AuthState.Authenticated -> {
          val navController = rememberNavController()
          NavHost(navController = navController, startDestination = "home") {

            // Screen 1: Home (adaptive navigation drawer shell around the household list)
            composable("home") {
              HomeScreen(
                onGroupClick = { id -> navController.navigate("groupProfile/$id") },
                onDataCaptureClick = {
                  navController.navigate(
                    questionnaireHostRoute(questionnaireId = QuestionnaireIds.HOUSEHOLD_REGISTRATION)
                  )
                },
                onSignOut = { authViewModel.logout() },
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
                      questionnaireId = QuestionnaireIds.HOUSEHOLD_MEMBERS,
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
                      questionnaireId = QuestionnaireIds.PATIENT_CLINICAL_DATA,
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
