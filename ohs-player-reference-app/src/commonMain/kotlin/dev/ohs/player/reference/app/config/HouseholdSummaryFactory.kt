package dev.ohs.player.reference.app.config

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ohs.player.generated.HouseholdSummaryState
import dev.ohs.player.library.config.ViewComponent
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

class HouseholdSummaryFactory(
    override val component: ViewComponent,
    private val onClick: () -> Unit
) : ViewComponentFactory<HouseholdSummaryState> {
    
    @Composable
    override fun Render(state: HouseholdSummaryState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            ListItem(
                headlineContent = { 
                    Text(
                        text = state.householdName ?: "Unknown Household",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                supportingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${state.memberCount ?: "0"} Members",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View Profile"
                    )
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
