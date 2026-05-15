package dev.ohs.player.reference.app.feature.patient.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.ohs.player.reference.app.data.model.PatientView
import dev.ohs.player.reference.app.feature.component.common.CardView
import dev.ohs.player.reference.app.feature.component.common.StatusChip

data class PatientCardConfig(
    val showStatusChip: Boolean = true,
    val showGender: Boolean = true,
    val showBirthDate: Boolean = true,
    val showLastVisit: Boolean = true,
    val elevationDp: Float = 2f,
    val contentPaddingDp: Float = 16f,
)

@Composable
fun PatientCard(
    patient: PatientView,
    config: PatientCardConfig = PatientCardConfig(),
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    CardView(
        elevationDp = config.elevationDp,
        contentPaddingDp = config.contentPaddingDp,
        onClick = onClick,
    ) {
        header {
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
        }
        body {
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
