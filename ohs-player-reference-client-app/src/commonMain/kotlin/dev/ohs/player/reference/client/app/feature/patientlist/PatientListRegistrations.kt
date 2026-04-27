package dev.ohs.player.reference.client.app.feature.patientlist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import dev.ohs.player.reference.client.app.data.model.PatientView
import dev.ohs.player.reference.client.app.feature.component.common.viewtypes.CardViewType
import dev.ohs.player.reference.client.app.feature.component.common.viewtypes.GridViewType
import dev.ohs.player.reference.client.app.feature.component.common.viewtypes.HorizontalListViewType
import dev.ohs.player.reference.client.app.feature.component.common.viewtypes.VerticalListViewType
import dev.ohs.player.reference.client.library.layout.GridListRenderer
import dev.ohs.player.reference.client.library.layout.HorizontalListRenderer
import dev.ohs.player.reference.client.library.layout.VerticalListRenderer
import dev.ohs.player.reference.client.library.registry.ViewRegistry
import dev.ohs.player.reference.client.library.registry.registerItem
import dev.ohs.player.reference.client.library.registry.registerLayout

fun ViewRegistry.registerPatientList() {
    registerItem<PatientView>(CardViewType, PatientCardRenderer())

    registerLayout<PatientView>(
        VerticalListViewType,
        VerticalListRenderer(contentPadding = PaddingValues(16.dp), itemSpacing = 12.dp),
    )
    registerLayout<PatientView>(
        HorizontalListViewType,
        HorizontalListRenderer(contentPadding = PaddingValues(16.dp), itemSpacing = 12.dp),
    )
    registerLayout<PatientView>(GridViewType, GridListRenderer(itemSpacing = 12.dp))
}
