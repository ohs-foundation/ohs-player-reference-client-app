package dev.ohs.player.reference.app

import dev.ohs.player.reference.app.feature.patientlist.registerPatientList
import dev.ohs.player.reference.app.feature.patientprofile.registerPatientProfile
import dev.ohs.player.library.registry.ViewRegistry

fun buildAppViewRegistry(): ViewRegistry = ViewRegistry().apply {
    registerPatientList()
    registerPatientProfile()
}
