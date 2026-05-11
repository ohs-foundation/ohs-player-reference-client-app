package dev.ohs.player.reference.app.feature.patient.profile

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import dev.ohs.player.library.registry.LocalViewRegistry
import dev.ohs.player.reference.app.buildAppViewRegistry
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PatientProfileScreenTest {

    /**
     * End-to-end smoke for the detail flow: app registry → DetailScaffold → each registered
     * section renderer. The scaffold uses a LazyColumn, so off-screen sections aren't composed
     * until scrolled into view — scroll to each section before asserting it's in the tree.
     */
    @Test
    fun knownPatient_rendersNameAndAllSections() = runComposeUiTest {
        val registry = buildAppViewRegistry()
        setContent {
            CompositionLocalProvider(LocalViewRegistry provides registry) {
                MaterialTheme {
                    PatientProfileScreen(patientId = "p1", onBack = {})
                }
            }
        }

        val scrollable = onNode(hasScrollAction())
        listOf(
            "Amina Diallo",
            "Personal Information",
            "Medical Information",
            "Contact & Insurance",
        ).forEach { text ->
            scrollable.performScrollToNode(hasText(text))
            assertTrue(
                onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty(),
                "Expected to find '$text' after scrolling the patient profile",
            )
        }
    }
}
