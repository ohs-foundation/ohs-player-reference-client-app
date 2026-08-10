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
package dev.ohs.player.reference.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide "the local FHIR data changed" tick that screens observe to re-query. Both app writes
 * (questionnaire extraction) and completed syncs bump it: sync downloads write straight to the
 * engine database, bypassing the repository's own write path, so without this signal a register
 * would stay stale until the next app-side edit. WorkManager and `BGProcessingTask` run in the app
 * process, so this singleton reaches the foreground collectors from a background sync too.
 */
object DataChangeSignal {
  private val _revision = MutableStateFlow(0L)
  val revision: StateFlow<Long> = _revision.asStateFlow()

  fun notifyChanged() {
    _revision.value += 1
  }
}
