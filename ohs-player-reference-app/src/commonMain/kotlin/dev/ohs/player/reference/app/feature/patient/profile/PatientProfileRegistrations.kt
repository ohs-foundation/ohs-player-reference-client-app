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
