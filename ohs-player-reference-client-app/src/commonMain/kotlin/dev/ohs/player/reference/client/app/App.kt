package dev.ohs.player.reference.client.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.savedstate.read
import dev.ohs.player.reference.client.app.feature.patientlist.PatientListScreen
import dev.ohs.player.reference.client.app.feature.patientprofile.PatientProfileScreen

private const val PatientListRoute = "patientList"
private const val PatientProfileRoute = "patientProfile"
private const val PatientIdArg = "patientId"

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = PatientListRoute) {
            composable(PatientListRoute) {
                PatientListScreen(
                    onPatientClick = { id ->
                        navController.navigate("$PatientProfileRoute/$id")
                    },
                )
            }
            composable(
                route = "$PatientProfileRoute/{$PatientIdArg}",
                arguments = listOf(navArgument(PatientIdArg) { type = NavType.StringType }),
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.read { getString(PatientIdArg) }.orEmpty()
                PatientProfileScreen(
                    patientId = patientId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
