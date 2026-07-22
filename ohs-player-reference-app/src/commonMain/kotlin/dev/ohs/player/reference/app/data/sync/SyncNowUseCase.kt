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

import dev.ohs.fhir.sync.FhirSyncTask
import dev.ohs.fhir.sync.SyncJobStatus
import dev.ohs.fhir.sync.runSync

/**
 * Triggers a one-time sync and returns its terminal result. A dedicated interface (rather than
 * calling [runSync] directly) so [dev.ohs.player.reference.app.feature.home.HomeViewModel] can be
 * unit-tested against a fake — [runSync] is a top-level extension function that reaches into the
 * library's global `FhirEngineProvider` singleton, so no [FhirSyncTask] instance, real or fake,
 * makes it independently testable.
 */
fun interface SyncNowUseCase {
  suspend fun invoke(): SyncJobStatus
}

class RunSyncNowUseCase(private val fhirSyncTask: FhirSyncTask) : SyncNowUseCase {
  override suspend fun invoke(): SyncJobStatus = fhirSyncTask.runSync(taskName = null) {}
}
