/*
 * Copyright 2026 Open Health Stack Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.ohs.player.reference.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PatientView(
  val id: String,
  val firstName: String,
  val lastName: String,
  val gender: String,
  val birthDate: String,
  val phoneNumber: String,
  val address: AddressView,
  val bloodType: String,
  val allergies: List<String>,
  val conditions: List<String>,
  val medications: List<MedicationView>,
  val emergencyContact: EmergencyContactView,
  val insuranceProvider: String,
  val medicalRecordNumber: String,
  val lastVisitDate: String,
  val isActive: Boolean,
) {
  val fullName: String
    get() = "$firstName $lastName"
}

@Serializable
data class AddressView(val street: String, val city: String, val country: String) {
  val formatted: String
    get() = "$street, $city, $country"
}

@Serializable
data class MedicationView(val name: String, val dosage: String, val frequency: String)

@Serializable
data class EmergencyContactView(
  val name: String,
  val relationship: String,
  val phoneNumber: String,
)
