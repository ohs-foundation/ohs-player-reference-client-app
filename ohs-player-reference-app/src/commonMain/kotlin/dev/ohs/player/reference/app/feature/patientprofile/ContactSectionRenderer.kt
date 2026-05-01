package dev.ohs.player.reference.app.feature.patientprofile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ohs.player.library.renderer.Renderer
import dev.ohs.player.reference.app.data.model.PatientView

class ContactSectionRenderer : Renderer<PatientView> {
    @Composable
    override fun Render(item: PatientView, onClick: () -> Unit, modifier: Modifier) {
        ContactSection(item)
    }
}
