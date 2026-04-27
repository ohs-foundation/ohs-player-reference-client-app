package dev.ohs.player.reference.client.app.feature.patientprofile

import dev.ohs.player.reference.client.app.data.model.PatientView
import dev.ohs.player.reference.client.app.feature.patientlist.PatientCardConfig
import dev.ohs.player.reference.client.app.feature.patientlist.PatientCardRenderer
import dev.ohs.player.reference.client.library.registry.ViewRegistry
import dev.ohs.player.reference.client.library.registry.registerItem

fun ViewRegistry.registerPatientProfile() {
    registerItem<PatientView>(
        PatientHeaderViewType,
        PatientCardRenderer(PatientCardConfig(showLastVisit = false)),
    )
    registerItem<PatientView>(PersonalSectionViewType, PersonalSectionRenderer())
    registerItem<PatientView>(MedicalSectionViewType, MedicalSectionRenderer())
    registerItem<PatientView>(ContactSectionViewType, ContactSectionRenderer())
}
