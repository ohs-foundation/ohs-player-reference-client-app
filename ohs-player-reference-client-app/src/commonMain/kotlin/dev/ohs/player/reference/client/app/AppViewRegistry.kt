package dev.ohs.player.reference.client.app

import dev.ohs.player.reference.client.app.feature.patientlist.registerPatientList
import dev.ohs.player.reference.client.app.feature.patientprofile.registerPatientProfile
import dev.ohs.player.reference.client.library.registry.ViewRegistry

fun buildAppViewRegistry(): ViewRegistry = ViewRegistry().apply {
    registerPatientList()
    registerPatientProfile()
}
