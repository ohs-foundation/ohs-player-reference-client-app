package dev.ohs.player.reference.app.feature.patient.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.reference.app.data.model.PatientView

class PatientCardRenderer : ComponentRenderer<PatientView, PatientCardConfig> {
    @Composable
    override fun Render(item: PatientView, config: PatientCardConfig, onClick: () -> Unit, modifier: Modifier) {
        PatientCard(
            patient = item,
            config = config,
            onClick = onClick,
            modifier = modifier,
        )
    }
}
