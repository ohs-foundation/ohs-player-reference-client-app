package dev.ohs.player.reference.app.feature.patientprofile

import dev.ohs.player.library.registry.ViewRegistry
import dev.ohs.player.library.registry.registerComponent
import dev.ohs.player.reference.app.AppViewTypes
import dev.ohs.player.reference.app.feature.patientlist.PatientCardConfig
import dev.ohs.player.reference.app.feature.patientlist.PatientCardRenderer

fun ViewRegistry.registerPatientProfile() {
    registerComponent(
        AppViewTypes.PatientHeader,
        PatientCardRenderer(),
        PatientCardConfig(showLastVisit = false),
    )
    registerComponent(AppViewTypes.PersonalSection, PersonalSectionRenderer(), PersonalSectionConfig)
    registerComponent(AppViewTypes.MedicalSection, MedicalSectionRenderer(), MedicalSectionConfig)
    registerComponent(AppViewTypes.ContactSection, ContactSectionRenderer(), ContactSectionConfig)
}
