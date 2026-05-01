package dev.ohs.player.reference.app.feature.patientprofile

import dev.ohs.player.library.registry.ViewRegistry
import dev.ohs.player.library.registry.registerItem
import dev.ohs.player.reference.app.data.model.PatientView
import dev.ohs.player.reference.app.feature.patientlist.PatientCardConfig
import dev.ohs.player.reference.app.feature.patientlist.PatientCardRenderer

fun ViewRegistry.registerPatientProfile() {
    registerItem<PatientView>(
        PatientHeaderViewType,
        PatientCardRenderer(PatientCardConfig(showLastVisit = false)),
    )
    registerItem<PatientView>(PersonalSectionViewType, PersonalSectionRenderer())
    registerItem<PatientView>(MedicalSectionViewType, MedicalSectionRenderer())
    registerItem<PatientView>(ContactSectionViewType, ContactSectionRenderer())
}
