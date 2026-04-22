package dev.ohs.player.reference.client.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.ohs.player.reference.client.app.data.model.PatientView
import dev.ohs.player.reference.client.app.feature.component.patient.ui.PatientCard
import dev.ohs.player.reference.client.app.feature.component.common.VerticalListLayout
import dev.ohs.player.reference.client.app.feature.patientlist.PatientCardViewType
import dev.ohs.player.reference.client.app.feature.patientlist.PatientListScreen
import dev.ohs.player.reference.client.app.feature.patientlist.VerticalListViewType
import dev.ohs.player.reference.client.library.registry.ViewRegistry

@Composable
fun App() {
    val registry = remember {
        ViewRegistry().apply {
            registerListItem<PatientView>(PatientCardViewType) { patient, onClick ->
                PatientCard(patient = patient, onClick = onClick)
            }
            registerLayout<PatientView>(VerticalListViewType, VerticalListLayout())
        }
    }

    MaterialTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "patientList") {
            composable("patientList") {
                PatientListScreen(
                    onPatientClick = { id -> },
                    registry = registry,
                )
            }
        }
    }
}
