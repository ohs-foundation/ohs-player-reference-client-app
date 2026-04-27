package dev.ohs.player.reference.client.app.feature.patientprofile

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ohs.player.reference.client.app.data.model.PatientView
import dev.ohs.player.reference.client.library.scaffold.DetailScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileScreen(
    patientId: String,
    onBack: () -> Unit,
) {
    val viewModel = remember(patientId) { PatientProfileViewModel(patientId) }
    val patient by viewModel.patient.collectAsStateWithLifecycle()

    DetailScaffold<PatientView>(item = patient) {
        topBar {
            TopAppBar(
                title = { Text(patient?.fullName ?: "Patient") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← Back", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
        notFound { Text("Patient not found") }
        section(PatientHeaderViewType)
        section(PersonalSectionViewType)
        section(MedicalSectionViewType)
        section(ContactSectionViewType)
    }
}
