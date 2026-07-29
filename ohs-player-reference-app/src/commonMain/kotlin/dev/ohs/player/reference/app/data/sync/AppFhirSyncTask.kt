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

import dev.ohs.fhir.engine.FhirEngine
import dev.ohs.fhir.engine.sync.AcceptLocalConflictResolver
import dev.ohs.fhir.engine.sync.ConflictResolver
import dev.ohs.fhir.engine.sync.DownloadWorkManager
import dev.ohs.fhir.engine.sync.FhirSyncTask
import dev.ohs.fhir.engine.sync.download.ResourceParamsBasedDownloadWorkManager
import dev.ohs.fhir.engine.sync.download.ResourceSearchParams
import dev.ohs.fhir.engine.sync.upload.HttpCreateMethod
import dev.ohs.fhir.engine.sync.upload.HttpUpdateMethod
import dev.ohs.fhir.engine.sync.upload.UploadStrategy
import dev.ohs.fhir.model.r4.terminologies.ResourceType

/**
 * Resource types downloaded on every sync, with their search parameters. Empty parameters mean
 * "everything of this type, since the last sync". Adding a resource type later is a new map entry.
 */
private val SYNC_RESOURCE_PARAMS: ResourceSearchParams =
  mapOf(ResourceType.Patient to emptyMap(), ResourceType.Group to emptyMap())

const val SYNC_TIMEOUT_DURATION = 120L

/**
 * This app's [FhirSyncTask]: downloads [SYNC_RESOURCE_PARAMS], resolves conflicts in favor of the
 * local change, and uploads pending local changes as a single bundle request.
 */
class AppFhirSyncTask(private val fhirEngine: FhirEngine) : FhirSyncTask {
  private val timestampContext = DataStoreTimestampContext(createSyncTimestampDataStore())

  override fun getFhirEngine(): FhirEngine = fhirEngine

  override fun getDownloadWorkManager(): DownloadWorkManager =
    ResourceParamsBasedDownloadWorkManager(SYNC_RESOURCE_PARAMS, timestampContext)

  override fun getConflictResolver(): ConflictResolver = AcceptLocalConflictResolver

  override fun getUploadStrategy(): UploadStrategy =
    UploadStrategy.forBundleRequest(
      methodForCreate = HttpCreateMethod.PUT,
      methodForUpdate = HttpUpdateMethod.PATCH,
      squash = true,
      bundleSize = 500,
    )
}
