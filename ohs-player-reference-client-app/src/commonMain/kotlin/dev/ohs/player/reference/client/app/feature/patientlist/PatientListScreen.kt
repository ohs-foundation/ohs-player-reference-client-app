package dev.ohs.player.reference.client.app.feature.patientlist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ohs.player.reference.client.app.data.model.PatientView
import dev.ohs.player.reference.client.library.registry.ViewRegistry
import dev.ohs.player.reference.client.library.registry.ViewType

val PatientCardViewType = ViewType("PatientCard")
val VerticalListViewType = ViewType("VerticalList")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    onPatientClick: (String) -> Unit,
    registry: ViewRegistry,
    listItemType: ViewType = PatientCardViewType,
    layoutType: ViewType = VerticalListViewType,
    viewModel: PatientListViewModel = viewModel { PatientListViewModel() },
) {
    val patients by viewModel.patients.collectAsStateWithLifecycle()

    val itemFactory = requireNotNull(registry.getListItem<PatientView>(listItemType)) {
        "No ListItemViewFactory registered for $listItemType"
    }
    val layoutFactory = requireNotNull(registry.getLayout<PatientView>(layoutType)) {
        "No LayoutViewFactory registered for $layoutType"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patients") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        layoutFactory.Content(
            items = patients,
            key = { it.id },
            itemContent = { patient ->
                itemFactory.Content(patient) { onPatientClick(patient.id) }
            },
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}
