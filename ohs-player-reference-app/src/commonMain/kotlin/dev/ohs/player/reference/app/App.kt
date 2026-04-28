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
import dev.ohs.player.reference.app.feature.patientlist.PatientListScreen
import dev.ohs.player.reference.app.feature.patientprofile.PatientProfileScreen
import dev.ohs.player.library.registry.LocalViewRegistry

private const val PatientListRoute = "patientList"
private const val PatientProfileRoute = "patientProfile"
private const val PatientIdArg = "patientId"

@Composable
fun App() {
    val registry = remember { buildAppViewRegistry() }

    CompositionLocalProvider(LocalViewRegistry provides registry) {
        MaterialTheme {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = PatientListRoute) {
                composable(PatientListRoute) {
                    PatientListScreen(
                        onPatientClick = { id -> navController.navigate("$PatientProfileRoute/$id") },
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
}
