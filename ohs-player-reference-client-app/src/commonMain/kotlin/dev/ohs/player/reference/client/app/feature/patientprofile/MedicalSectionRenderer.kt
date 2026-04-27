package dev.ohs.player.reference.client.app.feature.patientprofile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ohs.player.reference.client.app.data.model.PatientView
import dev.ohs.player.reference.client.library.renderer.Renderer

class MedicalSectionRenderer : Renderer<PatientView> {
    @Composable
    override fun Render(item: PatientView, onClick: () -> Unit, modifier: Modifier) {
        MedicalSection(item)
    }
}
