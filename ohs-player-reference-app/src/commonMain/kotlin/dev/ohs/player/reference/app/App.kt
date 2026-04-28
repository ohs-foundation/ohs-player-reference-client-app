package dev.ohs.player.reference.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.ohs.fhir.fhirpath.FhirPathEngine
import dev.ohs.player.generated.*
import dev.ohs.player.library.config.ViewOrchestrator
import dev.ohs.player.library.tranformer.DataTransformer
import dev.ohs.player.reference.app.config.HouseholdDashboardFactory
import dev.ohs.player.reference.app.config.HouseholdSummaryFactory
import dev.ohs.player.reference.app.config.IpsDashboardFactory
import dev.ohs.player.reference.app.config.ViewFactoryRegistry
import dev.ohs.player.reference.app.data.HouseholdRepository
import dev.ohs.player.reference.app.ui.Screen
import dev.ohs.player.reference.app.ui.components.OhsTopAppBar
import dev.ohs.player.reference.app.ui.list.HouseholdListScreen
import dev.ohs.player.reference.app.ui.list.HouseholdListViewModel
import dev.ohs.player.reference.app.ui.profile.HouseholdProfileScreen
import dev.ohs.player.reference.app.ui.profile.HouseholdProfileViewModel

val fhirPathEngine = FhirPathEngine.forR4()
val dataTransformer = DataTransformer(fhirPathEngine)
val orchestrator = ViewOrchestrator(dataTransformer)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val repository = remember { HouseholdRepository() }
    val listViewModel = remember { HouseholdListViewModel(orchestrator, repository) }
    val profileViewModel = remember { HouseholdProfileViewModel(orchestrator, repository) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.HouseholdList) }

    val registry = remember {
        ViewFactoryRegistry().apply {
            register<HouseholdSummaryState>(type = HouseholdSummary) { component, onClick ->
                HouseholdSummaryFactory(component, onClick ?: {})
            }
            register<HouseholdDashboardState>(type = HouseholdDashboard) { component, _ ->
                HouseholdDashboardFactory(component)
            }
            register<IpsDashboardState>(type = IpsDashboard) { component, _ ->
                IpsDashboardFactory(component)
            }
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = { 
                OhsTopAppBar(
                    title = when(currentScreen) {
                        is Screen.HouseholdList -> "Households"
                        is Screen.HouseholdProfile -> profileViewModel.state.householdName ?: "Household Profile"
                    },
                    onBackClick = if (currentScreen is Screen.HouseholdProfile) {
                        { 
                            profileViewModel.clear()
                            currentScreen = Screen.HouseholdList 
                        }
                    } else null
                ) 
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (currentScreen) {
                    is Screen.HouseholdList -> {
                        HouseholdListScreen(
                            registry = registry,
                            state = listViewModel.state,
                            onHouseholdClick = { id -> 
                                profileViewModel.loadHousehold(id)
                                currentScreen = Screen.HouseholdProfile(id) 
                            }
                        )
                    }
                    is Screen.HouseholdProfile -> {
                        HouseholdProfileScreen(
                            registry = registry,
                            state = profileViewModel.state
                        )
                    }
                }
            }
        }
    }
}