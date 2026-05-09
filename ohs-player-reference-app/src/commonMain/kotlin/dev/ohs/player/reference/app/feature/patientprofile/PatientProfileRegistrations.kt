package dev.ohs.player.reference.app.feature.patientprofile

import dev.ohs.player.library.registry.ViewRegistry
import dev.ohs.player.library.registry.registerComponent
import dev.ohs.player.reference.app.AppViewTypes
import dev.ohs.player.reference.app.data.model.PatientView
import dev.ohs.player.reference.app.feature.patientlist.PatientCardConfig
import dev.ohs.player.reference.app.feature.patientlist.PatientCardRenderer

fun ViewRegistry.registerPatientProfile() {
    registerComponent<PatientView>(
        AppViewTypes.PatientHeader,
        PatientCardRenderer(PatientCardConfig(showLastVisit = false)),
    )
    registerComponent<PatientView>(AppViewTypes.PersonalSection, PersonalSectionRenderer())
    registerComponent<PatientView>(AppViewTypes.MedicalSection, MedicalSectionRenderer())
    registerComponent<PatientView>(AppViewTypes.ContactSection, ContactSectionRenderer())
}
