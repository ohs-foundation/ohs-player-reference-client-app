package dev.ohs.player.reference.app

import dev.ohs.player.library.registry.componentRenderer
import dev.ohs.player.library.registry.layoutRenderer
import dev.ohs.player.reference.app.data.model.PatientView
import kotlin.test.Test

class AppViewRegistryTest {

    /**
     * The screens declare their layout via `component(...)` / `layout(...)` / `section(...)` calls
     * that resolve from the registry at render time. A missed registration only surfaces
     * when a user actually opens the screen, this test catches it ahead of that.
     */
    @Test
    fun allRequiredRenderersAreRegistered() {
        val registry = buildAppViewRegistry()
        // Each call throws if the registry is missing an entry.

        // Patient list, component + every layout the screen may pick.
        registry.componentRenderer<PatientView>(AppViewTypes.Card)
        registry.layoutRenderer<PatientView>(AppViewTypes.VerticalList)
        registry.layoutRenderer<PatientView>(AppViewTypes.HorizontalList)
        registry.layoutRenderer<PatientView>(AppViewTypes.Grid)

        // Patient profile, header + each section the DetailScaffold composes.
        registry.componentRenderer<PatientView>(AppViewTypes.PatientHeader)
        registry.componentRenderer<PatientView>(AppViewTypes.PersonalSection)
        registry.componentRenderer<PatientView>(AppViewTypes.MedicalSection)
        registry.componentRenderer<PatientView>(AppViewTypes.ContactSection)
    }
}
