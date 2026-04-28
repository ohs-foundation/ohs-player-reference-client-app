package dev.ohs.player.reference.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ohs.player.library.config.ViewComponent
import dev.ohs.player.generated.HouseholdDashboard
import dev.ohs.player.generated.IpsDashboard
import dev.ohs.player.reference.app.config.ViewFactoryRegistry
import kotlinx.serialization.json.JsonElement

@Composable
fun HouseholdProfileScreen(
    registry: ViewFactoryRegistry,
    state: HouseholdProfileUiState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            if (state.householdProfile != null) {
                registry.RenderComponent(ViewComponent("profile", HouseholdDashboard), state.householdProfile)
            }
        }
        item {
            if (state.ipsDashboard != null) {
                registry.RenderComponent(ViewComponent("ips", IpsDashboard), state.ipsDashboard)
            }
        }
    }
}
