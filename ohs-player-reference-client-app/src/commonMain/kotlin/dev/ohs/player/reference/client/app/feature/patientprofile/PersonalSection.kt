package dev.ohs.player.reference.client.app.feature.patientprofile

import androidx.compose.runtime.Composable
import dev.ohs.player.reference.client.app.data.model.PatientView
import dev.ohs.player.reference.client.app.feature.component.common.InfoRow
import dev.ohs.player.reference.client.app.feature.component.common.SectionCard

@Composable
fun PersonalSection(patient: PatientView) {
    SectionCard(title = "Personal Information") {
        InfoRow("Full Name", patient.fullName)
        InfoRow("Gender", patient.gender)
        InfoRow("Date of Birth", patient.birthDate)
        InfoRow("Phone", patient.phoneNumber)
        InfoRow("Address", patient.address.formatted)
        InfoRow("Status", if (patient.isActive) "Active" else "Inactive")
    }
}
