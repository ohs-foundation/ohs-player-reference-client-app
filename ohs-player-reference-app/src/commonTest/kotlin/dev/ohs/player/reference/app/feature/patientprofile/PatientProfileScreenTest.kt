package dev.ohs.player.reference.app.feature.patientprofile

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runComposeUiTest
import dev.ohs.player.library.registry.LocalViewRegistry
import dev.ohs.player.reference.app.buildAppViewRegistry
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PatientProfileScreenTest {

    /**
     * End-to-end smoke for the detail flow: app registry → DetailScaffold → each registered
     * section renderer. Sections may render off-screen in the test viewport, so this asserts
     * existence in the semantic tree rather than visibility.
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

        assertTrue(onAllNodesWithText("Amina Diallo").fetchSemanticsNodes().isNotEmpty())
        onAllNodesWithText("Personal Information").assertCountEquals(1)
        onAllNodesWithText("Medical Information").assertCountEquals(1)
        onAllNodesWithText("Contact & Insurance").assertCountEquals(1)
    }
}
