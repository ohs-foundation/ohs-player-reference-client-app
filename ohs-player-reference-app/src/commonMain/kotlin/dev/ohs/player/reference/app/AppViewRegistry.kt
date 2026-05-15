package dev.ohs.player.reference.app

import dev.ohs.player.library.registry.ViewRegistry
import dev.ohs.player.reference.app.feature.patient.list.registerPatientList
import dev.ohs.player.reference.app.feature.patient.profile.registerPatientProfile

fun buildAppViewRegistry(): ViewRegistry = ViewRegistry().apply {
    registerPatientList()
    registerPatientProfile()
}
