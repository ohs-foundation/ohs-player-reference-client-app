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
package dev.ohs.player.reference.app.data.sync

import dev.ohs.fhir.model.r4.terminologies.ResourceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class InMemoryTimestampContextTest {

  @Test
  fun getLasUpdateTimestamp_beforeAnySave_isNull() = runTest {
    val context = InMemoryTimestampContext()

    assertNull(context.getLasUpdateTimestamp(ResourceType.Patient))
  }

  @Test
  fun saveThenGet_roundTripsPerResourceType() = runTest {
    val context = InMemoryTimestampContext()

    context.saveLastUpdatedTimestamp(ResourceType.Patient, "2026-07-15T10:00:00Z")
    context.saveLastUpdatedTimestamp(ResourceType.Group, "2026-07-14T09:00:00Z")

    assertEquals("2026-07-15T10:00:00Z", context.getLasUpdateTimestamp(ResourceType.Patient))
    assertEquals("2026-07-14T09:00:00Z", context.getLasUpdateTimestamp(ResourceType.Group))
  }

  @Test
  fun saveWithNullTimestamp_doesNotOverwriteExistingValue() = runTest {
    val context = InMemoryTimestampContext()
    context.saveLastUpdatedTimestamp(ResourceType.Patient, "2026-07-15T10:00:00Z")

    context.saveLastUpdatedTimestamp(ResourceType.Patient, null)

    assertEquals("2026-07-15T10:00:00Z", context.getLasUpdateTimestamp(ResourceType.Patient))
  }
}
