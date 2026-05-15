package dev.ohs.player.reference.app

import androidx.compose.material3.MaterialTheme
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
import dev.ohs.player.reference.app.feature.patient.list.PatientListScreen
import dev.ohs.player.reference.app.feature.patient.profile.PatientProfileScreen

private const val PATIENT_LIST_ROUTE = "patientList"
private const val PATIENT_PROFILE_ROUTE = "patientProfile"
private const val PATIENT_ID_ARG = "patientId"

@Composable
fun App() {
    val registry = remember { buildAppViewRegistry() }

    CompositionLocalProvider(LocalViewRegistry provides registry) {
        MaterialTheme {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = PATIENT_LIST_ROUTE) {
                composable(PATIENT_LIST_ROUTE) {
                    PatientListScreen(
                        onPatientClick = { id -> navController.navigate("$PATIENT_PROFILE_ROUTE/$id") },
                    )
                }
                composable(
                    route = "$PATIENT_PROFILE_ROUTE/{$PATIENT_ID_ARG}",
                    arguments = listOf(navArgument(PATIENT_ID_ARG) { type = NavType.StringType }),
                ) { backStackEntry ->
                    val patientId = backStackEntry.arguments?.read { getString(PATIENT_ID_ARG) }.orEmpty()
                    PatientProfileScreen(
                        patientId = patientId,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
