package dev.ohs.player.reference.app.feature.patient.profile

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ohs.player.reference.app.data.model.PatientView
import dev.ohs.player.reference.app.feature.component.common.InfoRow
import dev.ohs.player.reference.app.feature.component.common.SectionCard

@Composable
fun ContactSection(patient: PatientView) {
    SectionCard(title = "Contact & Insurance") {
        Text("Emergency Contact", style = MaterialTheme.typography.labelMedium)
        InfoRow("Name", patient.emergencyContact.name)
        InfoRow("Relationship", patient.emergencyContact.relationship)
        InfoRow("Phone", patient.emergencyContact.phoneNumber)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        InfoRow("Insurance", patient.insuranceProvider)
    }
}
