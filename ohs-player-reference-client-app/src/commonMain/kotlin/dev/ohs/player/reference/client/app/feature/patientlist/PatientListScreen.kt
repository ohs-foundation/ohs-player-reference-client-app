package dev.ohs.player.reference.client.app.feature.patientlist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ohs.player.reference.client.library.layout.VerticalListRenderer
import dev.ohs.player.reference.client.library.scaffold.ListScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(onPatientClick: (String) -> Unit) {
    val viewModel: PatientListViewModel = viewModel { PatientListViewModel() }
    val patients by viewModel.patients.collectAsStateWithLifecycle()

    ListScaffold(
        items = patients,
        onItemClick = { onPatientClick(it.id) },
        key = { it.id },
    ) {
        item(PatientCardRenderer())
        layout(VerticalListRenderer(contentPadding = PaddingValues(16.dp), itemSpacing = 12.dp))
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
