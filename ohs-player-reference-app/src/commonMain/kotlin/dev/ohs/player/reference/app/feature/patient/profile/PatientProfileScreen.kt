/*
 * Copyright 2026 Open Health Stack Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        colors =
          TopAppBarDefaults.topAppBarColors(
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
