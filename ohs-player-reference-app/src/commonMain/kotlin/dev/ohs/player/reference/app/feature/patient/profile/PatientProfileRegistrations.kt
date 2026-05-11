package dev.ohs.player.reference.app.feature.patient.profile

import dev.ohs.player.library.registry.ViewRegistry
import dev.ohs.player.library.registry.registerComponent
import dev.ohs.player.reference.app.AppViewTypes
import dev.ohs.player.reference.app.feature.patient.list.PatientCardConfig
import dev.ohs.player.reference.app.feature.patient.list.PatientCardRenderer

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
