package dev.ohs.player.reference.app.feature.patientprofile

import dev.ohs.player.library.registry.ViewType

data object PatientHeaderViewType : ViewType {
    override val value = "PatientHeader"
}
data object PersonalSectionViewType : ViewType {
    override val value = "PersonalSection"
}
data object MedicalSectionViewType : ViewType {
    override val value = "MedicalSection"
}
data object ContactSectionViewType : ViewType {
    override val value = "ContactSection"
}
