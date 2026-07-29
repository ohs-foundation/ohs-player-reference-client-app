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

import android.content.Context
import androidx.work.WorkerParameters
import dev.ohs.fhir.engine.FhirEngineProvider
import dev.ohs.fhir.engine.sync.ConflictResolver
import dev.ohs.fhir.engine.sync.DownloadWorkManager
import dev.ohs.fhir.engine.sync.FhirSyncWorker
import dev.ohs.fhir.engine.sync.upload.UploadStrategy
import dev.ohs.player.reference.app.auth.ensureFreshSessionForSync

/**
 * WorkManager entry point for this app's sync, enqueued via [dev.ohs.fhir.engine.sync.Sync]. Built
 * by WorkManager's default factory (reflection on the `(Context, WorkerParameters)` constructor),
 * so it constructs its own [AppFhirSyncTask] rather than resolving one through Koin.
 */
class AppFhirSyncWorker(appContext: Context, workerParams: WorkerParameters) :
  FhirSyncWorker(appContext, workerParams) {
  private val syncTask = AppFhirSyncTask(FhirEngineProvider.getInstance(appContext))

  /**
   * WorkManager can relaunch this worker in a fresh process after the app was killed, where the UI
   * bootstrap never ran and [dev.ohs.player.reference.app.auth.SessionRepository] is empty. Hydrate
   * and refresh the session first so the sync's requests carry a valid Bearer token.
   */
  override suspend fun doWork(): Result {
    ensureFreshSessionForSync()
    return super.doWork()
  }

  override fun getFhirEngine() = syncTask.getFhirEngine()

  override fun getDownloadWorkManager(): DownloadWorkManager = syncTask.getDownloadWorkManager()

  override fun getConflictResolver(): ConflictResolver = syncTask.getConflictResolver()

  override fun getUploadStrategy(): UploadStrategy = syncTask.getUploadStrategy()
}
