package dev.ohs.player.reference.app.feature.patient.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import dev.ohs.player.library.layout.GridListRenderer
import dev.ohs.player.library.layout.HorizontalListRenderer
import dev.ohs.player.library.layout.VerticalListRenderer
import dev.ohs.player.library.registry.ViewRegistry
import dev.ohs.player.library.registry.registerComponent
import dev.ohs.player.library.registry.registerLayout
import dev.ohs.player.reference.app.AppViewTypes
import dev.ohs.player.reference.app.data.model.PatientView

fun ViewRegistry.registerPatientList() {
    registerComponent<PatientView, PatientCardConfig>(
        AppViewTypes.Card,
        PatientCardRenderer(),
        PatientCardConfig(),
    )

    registerLayout<PatientView>(
        AppViewTypes.VerticalList,
        VerticalListRenderer(contentPadding = PaddingValues(16.dp), itemSpacing = 12.dp),
    )
    registerLayout<PatientView>(
        AppViewTypes.HorizontalList,
        HorizontalListRenderer(contentPadding = PaddingValues(16.dp), itemSpacing = 12.dp),
    )
    registerLayout<PatientView>(AppViewTypes.Grid, GridListRenderer(itemSpacing = 12.dp))
}
