package dev.ohs.player.reference.app.feature.patient.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.reference.app.data.model.PatientView

data object PersonalSectionConfig

class PersonalSectionRenderer : ComponentRenderer<PatientView, PersonalSectionConfig> {
    @Composable
    override fun Render(item: PatientView, config: PersonalSectionConfig, onClick: () -> Unit, modifier: Modifier) {
        PersonalSection(item)
    }
}
