package dev.ohs.player.reference.app.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.ohs.player.generated.IpsDashboardState
import dev.ohs.player.generated.MedicationItemState
import dev.ohs.player.library.config.ViewComponent

class IpsDashboardFactory(override val component: ViewComponent) : ViewComponentFactory<IpsDashboardState> {

  @Composable
  override fun Render(state: IpsDashboardState) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Patient Context Header
      Row(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.AccountCircle,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "${state.header?.familyName ?: "Unknown Patient"}'s Medications",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary
        )
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
      ) {
        Column {
          state.medications.forEachIndexed { index, med ->
            MedicationItem(med)
            if (index < state.medications.size - 1) {
              HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
              )
            }
          }
        }
      }
    }
  }

  @Composable
  fun MedicationItem(state: MedicationItemState) {
    ListItem(
      headlineContent = {
        Text(
          text = state.medName ?: "Unknown Medication",
          style = MaterialTheme.typography.bodyLarge
        )
      },
      supportingContent = {
        Text(
          text = state.status?.uppercase() ?: "UNKNOWN",
          style = MaterialTheme.typography.labelSmall,
          color = if (state.status == "active") 
              MaterialTheme.colorScheme.primary 
          else 
              MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      colors = ListItemDefaults.colors(
        containerColor = Color.Transparent
      )
    )
  }
}

@Composable
fun IpsHeader(state: IpsDashboardState) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
      Column {
        // familyName matches PatientHeaderView select.column[1].name from FSH
        Text(text = state.header?.familyName ?: "Unknown Patient", style = MaterialTheme.typography.headlineSmall)
        Text(text = "ID: ${state.header?.patientId ?: "Unknown ID"}")
      }

      if (state.medications.isNotEmpty()) {
        SuggestionChip(
          onClick = { },
          label = { Text("Active Meds: ${state.medications.size}") },
          colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        )
      }
    }
  }
}