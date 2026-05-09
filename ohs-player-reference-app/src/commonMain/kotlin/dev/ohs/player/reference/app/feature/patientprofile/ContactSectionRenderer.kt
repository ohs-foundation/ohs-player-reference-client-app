package dev.ohs.player.reference.app.feature.patientprofile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.reference.app.data.model.PatientView

class ContactSectionRenderer : ComponentRenderer<PatientView> {
    @Composable
    override fun Render(item: PatientView, onClick: () -> Unit, modifier: Modifier) {
        ContactSection(item)
    }
}
