package dev.ohs.player.reference.app

import dev.ohs.player.library.registry.ViewType

object AppViewTypes {
    // Component styles
    val Card = ViewType("Card")
    val Row = ViewType("Row")

    // Layout styles
    val VerticalList = ViewType("VerticalList")
    val HorizontalList = ViewType("HorizontalList")
    val Grid = ViewType("Grid")

    // Patient profile sections
    val PatientHeader = ViewType("PatientHeader")
    val PersonalSection = ViewType("PersonalSection")
    val MedicalSection = ViewType("MedicalSection")
    val ContactSection = ViewType("ContactSection")
}
