package dev.ohs.player.reference.client.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.ohs.player.reference.client.app.feature.patientlist.PatientListScreen

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "patientList") {
            composable("patientList") {
                PatientListScreen(
                    onPatientClick = { id ->  },
                )
            }
        }
    }
}
