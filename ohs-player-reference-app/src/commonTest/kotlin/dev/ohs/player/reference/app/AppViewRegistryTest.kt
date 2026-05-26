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
package dev.ohs.player.reference.app

import dev.ohs.player.library.registry.componentRenderer
import dev.ohs.player.library.registry.layoutRenderer
import dev.ohs.player.reference.app.data.model.PatientView
import kotlin.test.Test

class AppViewRegistryTest {

  /**
   * The screens declare their layout via `component(...)` / `layout(...)` / `section(...)` calls
   * that resolve from the registry at render time. A missed registration only surfaces when a user
   * actually opens the screen, this test catches it ahead of that.
   */
  @Test
  fun allRequiredRenderersAreRegistered() {
    val registry = buildAppViewRegistry()
    // Each call throws if the registry is missing an entry.

    // Patient list, component + every layout the screen may pick.
    registry.componentRenderer<PatientView>(AppViewTypes.Card)
    registry.layoutRenderer<PatientView>(AppViewTypes.VerticalList)
    registry.layoutRenderer<PatientView>(AppViewTypes.HorizontalList)
    registry.layoutRenderer<PatientView>(AppViewTypes.Grid)

    // Patient profile, header + each section the DetailScaffold composes.
    registry.componentRenderer<PatientView>(AppViewTypes.PatientHeader)
    registry.componentRenderer<PatientView>(AppViewTypes.PersonalSection)
    registry.componentRenderer<PatientView>(AppViewTypes.MedicalSection)
    registry.componentRenderer<PatientView>(AppViewTypes.ContactSection)
  }
}
