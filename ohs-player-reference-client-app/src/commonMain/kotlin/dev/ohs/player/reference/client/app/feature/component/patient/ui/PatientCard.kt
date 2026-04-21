package dev.ohs.player.reference.client.app.feature.component.patient.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ohs.player.reference.client.app.data.model.PatientView
import dev.ohs.player.reference.client.app.feature.component.common.StatusChip
import dev.ohs.player.reference.client.app.feature.component.patient.config.PatientCardConfig

@Composable
fun PatientCard(
    patient: PatientView,
    onClick: () -> Unit,
    config: PatientCardConfig = PatientCardConfig(),
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = config.elevationDp.dp),
    ) {
        Column(
            Modifier.padding(config.contentPaddingDp.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(patient.fullName, style = MaterialTheme.typography.titleMedium)
                if (config.showStatusChip) {
                    StatusChip(isActive = patient.isActive)
                }
            }
            val details = buildList {
                if (config.showGender) add(patient.gender)
                if (config.showBirthDate) add("Born: ${patient.birthDate}")
            }
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString("  •  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (config.showLastVisit) {
                Text(
                    text = "Last visit: ${patient.lastVisitDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
