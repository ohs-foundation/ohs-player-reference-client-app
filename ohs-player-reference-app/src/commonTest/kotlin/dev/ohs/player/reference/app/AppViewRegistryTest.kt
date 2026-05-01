package dev.ohs.player.reference.app

import dev.ohs.player.library.registry.itemRenderer
import dev.ohs.player.library.registry.layoutRenderer
import dev.ohs.player.reference.app.data.model.PatientView
import dev.ohs.player.reference.app.feature.component.common.viewtypes.CardViewType
import dev.ohs.player.reference.app.feature.component.common.viewtypes.GridViewType
import dev.ohs.player.reference.app.feature.component.common.viewtypes.HorizontalListViewType
import dev.ohs.player.reference.app.feature.component.common.viewtypes.VerticalListViewType
import dev.ohs.player.reference.app.feature.patientprofile.ContactSectionViewType
import dev.ohs.player.reference.app.feature.patientprofile.MedicalSectionViewType
import dev.ohs.player.reference.app.feature.patientprofile.PatientHeaderViewType
import dev.ohs.player.reference.app.feature.patientprofile.PersonalSectionViewType
import kotlin.test.Test
import kotlin.test.assertNotNull

class AppViewRegistryTest {

    /**
     * The screens declare their layout via `item(...)` / `layout(...)` / `section(...)` calls
     * that resolve from the registry at render time. A missed registration only surfaces
     * when a user actually opens the screen, this test catches it ahead of that.
     */
    @Test
    fun allRequiredRenderersAreRegistered() {
        val registry = buildAppViewRegistry()

        // Patient list, item + every layout the screen may pick.
        assertNotNull(registry.itemRenderer<PatientView>(CardViewType))
        assertNotNull(registry.layoutRenderer<PatientView>(VerticalListViewType))
        assertNotNull(registry.layoutRenderer<PatientView>(HorizontalListViewType))
        assertNotNull(registry.layoutRenderer<PatientView>(GridViewType))

        // Patient profile, header + each section the DetailScaffold composes.
        assertNotNull(registry.itemRenderer<PatientView>(PatientHeaderViewType))
        assertNotNull(registry.itemRenderer<PatientView>(PersonalSectionViewType))
        assertNotNull(registry.itemRenderer<PatientView>(MedicalSectionViewType))
        assertNotNull(registry.itemRenderer<PatientView>(ContactSectionViewType))
    }
}
