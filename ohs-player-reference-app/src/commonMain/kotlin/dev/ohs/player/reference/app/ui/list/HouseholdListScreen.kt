package dev.ohs.player.reference.app.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ohs.player.generated.HouseholdSummary
import dev.ohs.player.library.config.ViewComponent
import dev.ohs.player.reference.app.config.ViewFactoryRegistry
import kotlinx.serialization.json.JsonElement

@Composable
fun HouseholdListScreen(
    registry: ViewFactoryRegistry,
    state: HouseholdListUiState,
    onHouseholdClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(state.households) { (id, summaryJson) ->
            registry.RenderComponent(
                component = ViewComponent("item_$id", HouseholdSummary),
                data = summaryJson,
                onClick = { onHouseholdClick(id) }
            )
        }
    }
}
