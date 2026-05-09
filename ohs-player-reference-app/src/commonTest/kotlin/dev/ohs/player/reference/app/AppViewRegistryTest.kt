package dev.ohs.player.reference.app

import dev.ohs.player.library.registry.componentRenderer
import dev.ohs.player.library.registry.layoutRenderer
import dev.ohs.player.reference.app.data.model.PatientView
import kotlin.test.Test
import kotlin.test.assertNotNull

class AppViewRegistryTest {

    /**
     * The screens declare their layout via `component(...)` / `layout(...)` / `section(...)` calls
     * that resolve from the registry at render time. A missed registration only surfaces
     * when a user actually opens the screen, this test catches it ahead of that.
     */
    @Test
    fun allRequiredRenderersAreRegistered() {
        val registry = buildAppViewRegistry()

        // Patient list, component + every layout the screen may pick.
        assertNotNull(registry.componentRenderer<PatientView>(AppViewTypes.Card))
        assertNotNull(registry.layoutRenderer<PatientView>(AppViewTypes.VerticalList))
        assertNotNull(registry.layoutRenderer<PatientView>(AppViewTypes.HorizontalList))
        assertNotNull(registry.layoutRenderer<PatientView>(AppViewTypes.Grid))

        // Patient profile, header + each section the DetailScaffold composes.
        assertNotNull(registry.componentRenderer<PatientView>(AppViewTypes.PatientHeader))
        assertNotNull(registry.componentRenderer<PatientView>(AppViewTypes.PersonalSection))
        assertNotNull(registry.componentRenderer<PatientView>(AppViewTypes.MedicalSection))
        assertNotNull(registry.componentRenderer<PatientView>(AppViewTypes.ContactSection))
    }
}
