package dev.ohs.player.reference.client.app.feature.component.patient.config

data class PatientCardConfig(
    val showStatusChip: Boolean = true,
    val showGender: Boolean = true,
    val showBirthDate: Boolean = true,
    val showLastVisit: Boolean = true,
    val elevationDp: Float = 2f,
    val contentPaddingDp: Float = 16f,
)
