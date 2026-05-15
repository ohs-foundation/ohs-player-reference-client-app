package dev.ohs.player.reference.app.feature.patient.list

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.ohs.player.library.registry.LocalViewRegistry
import dev.ohs.player.reference.app.buildAppViewRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PatientListScreenTest {

    /**
     * End-to-end smoke for the list flow: app registry → ListScaffold → registered card
     * renderer → click handler. If any link in that chain breaks, this test fails.
     */
    @Test
    fun tappingPatient_invokesOnPatientClickWithMatchingId() = runComposeUiTest {
        val registry = buildAppViewRegistry()
        var clickedId: String? = null
        setContent {
            CompositionLocalProvider(LocalViewRegistry provides registry) {
                MaterialTheme {
                    PatientListScreen(onPatientClick = { clickedId = it })
                }
            }
        }

        onNodeWithText("Amina Diallo").performClick()
        assertEquals("p1", clickedId)
    }
}
