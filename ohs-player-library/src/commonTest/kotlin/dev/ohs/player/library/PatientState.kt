package dev.ohs.player.library

import kotlinx.serialization.Serializable

/**
 * Test state class representing extracted Patient fields.
 *
 * All fields are nullable with defaults. Each
[ViewDefinition] populates only
 * the fields it declares — unmapped fields remain null.
 */
@Serializable
data class PatientState(
    val patientId: String? = null,
    val familyName: String? = null,
    val fullName: String? = null,
    val gender: String? = null,
    val birthDate: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val city: String? = null,
    val country: String? = null,
)