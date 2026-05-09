package dev.ohs.player.reference.app.feature.patientlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.reference.app.data.model.PatientView

class PatientCardRenderer(private val config: PatientCardConfig = PatientCardConfig()) :
    ComponentRenderer<PatientView> {
    @Composable
    override fun Render(item: PatientView, onClick: () -> Unit, modifier: Modifier) {
        PatientCard(
            patient = item,
            config = config,
            onClick = onClick,
            modifier = modifier,
        )
    }
}
