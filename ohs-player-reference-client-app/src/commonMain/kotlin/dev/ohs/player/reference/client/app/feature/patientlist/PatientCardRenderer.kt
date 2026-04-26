package dev.ohs.player.reference.client.app.feature.patientlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ohs.player.reference.client.app.data.model.PatientView
import dev.ohs.player.reference.client.library.renderer.Renderer

class PatientCardRenderer(
    private val config: PatientCardConfig = PatientCardConfig(),
) : Renderer<PatientView> {
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
