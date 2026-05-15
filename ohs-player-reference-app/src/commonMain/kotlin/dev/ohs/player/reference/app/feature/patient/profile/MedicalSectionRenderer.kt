package dev.ohs.player.reference.app.feature.patient.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.reference.app.data.model.PatientView

data object MedicalSectionConfig

class MedicalSectionRenderer : ComponentRenderer<PatientView, MedicalSectionConfig> {
    @Composable
    override fun Render(item: PatientView, config: MedicalSectionConfig, onClick: () -> Unit, modifier: Modifier) {
        MedicalSection(item)
    }
}
