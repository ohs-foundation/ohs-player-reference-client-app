package dev.ohs.player.reference.app.ui.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.ohs.player.generated.HouseholdDashboardState
import dev.ohs.player.generated.viewDefinitions
import dev.ohs.player.library.config.ViewOrchestrator
import dev.ohs.player.reference.app.data.HouseholdRepository
import kotlinx.serialization.json.JsonElement

data class HouseholdListUiState(
    val households: List<Pair<String, JsonElement>> = emptyList()
)

class HouseholdListViewModel(
    private val orchestrator: ViewOrchestrator,
    private val repository: HouseholdRepository
) : ViewModel() {
    var state by mutableStateOf(HouseholdListUiState())
        private set

    init {
        loadHouseholds()
    }

    private fun loadHouseholds() {
        val results = repository.getAllHouseholds()
        val households = results.mapNotNull { result ->
            val id = result.resource.id ?: "unknown"
            val householdAssembled = orchestrator.assemble<HouseholdDashboardState>(result, viewDefinitions)
            val summaryJson = householdAssembled["summary"]
            if (summaryJson != null) id to summaryJson else null
        }
        state = state.copy(households = households)
    }
}
