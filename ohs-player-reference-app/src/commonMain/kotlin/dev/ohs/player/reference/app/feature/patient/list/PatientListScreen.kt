package dev.ohs.player.reference.app.feature.patient.list

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ohs.player.library.scaffold.ListScaffold
import dev.ohs.player.reference.app.AppViewTypes
import dev.ohs.player.reference.app.data.model.PatientView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(onPatientClick: (String) -> Unit) {
    val viewModel: PatientListViewModel = viewModel { PatientListViewModel() }
    val patients by viewModel.patients.collectAsStateWithLifecycle()

    ListScaffold<PatientView>(
        items = patients,
        onItemClick = { onPatientClick(it.id) },
        key = { it.id },
    ) {
        component(AppViewTypes.Card)
        layout(AppViewTypes.VerticalList)
        topBar {
            TopAppBar(
                title = { Text("Patients") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
        emptyState { Text("No patients") }
    }
}
