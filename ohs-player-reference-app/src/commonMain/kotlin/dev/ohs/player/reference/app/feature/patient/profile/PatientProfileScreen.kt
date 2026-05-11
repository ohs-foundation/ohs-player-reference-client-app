package dev.ohs.player.reference.app.feature.patient.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ohs.player.library.scaffold.DetailScaffold
import dev.ohs.player.reference.app.AppViewTypes
import dev.ohs.player.reference.app.data.model.PatientView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileScreen(patientId: String, onBack: () -> Unit) {
    val viewModel = remember(patientId) { PatientProfileViewModel(patientId) }
    val patient by viewModel.patient.collectAsStateWithLifecycle()

    DetailScaffold<PatientView>(item = patient) {
        topBar {
            TopAppBar(
                title = { Text(patient?.fullName ?: "Patient") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
        notFound { Text("Patient not found") }
        section(AppViewTypes.PatientHeader)
        section(AppViewTypes.PersonalSection)
        section(AppViewTypes.MedicalSection)
        section(AppViewTypes.ContactSection)
    }
}
