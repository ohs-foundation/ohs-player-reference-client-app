package dev.ohs.player.reference.app.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.ohs.player.generated.*
import dev.ohs.player.library.config.ViewOrchestrator
import dev.ohs.player.reference.app.data.HouseholdRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

data class HouseholdProfileUiState(
    val householdProfile: JsonElement? = null,
    val ipsDashboard: JsonElement? = null,
    val householdName: String? = null
)

class HouseholdProfileViewModel(
    private val orchestrator: ViewOrchestrator,
    private val repository: HouseholdRepository
) : ViewModel() {
    private val json = Json { ignoreUnknownKeys = true }
    var state by mutableStateOf(HouseholdProfileUiState())
        private set

    fun loadHousehold(id: String) {
        val result = repository.getHouseholdById(id) ?: return
        val householdAssembled = orchestrator.assemble<HouseholdDashboardState>(result, viewDefinitions)
        
        val summaryJson = householdAssembled.jsonObject["summary"]
        val summaryState = summaryJson?.let { json.decodeFromJsonElement<HouseholdSummaryState>(it) }
        
        state = state.copy(
            householdProfile = householdAssembled,
            householdName = summaryState?.householdName,
            ipsDashboard = orchestrator.assemble<IpsDashboardState>(result, viewDefinitions)
        )
    }

    fun clear() {
        state = HouseholdProfileUiState()
    }
}
