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
package dev.ohs.player.reference.app.data.repository

interface RepositorySnapshotStore {
  suspend fun read(): String?

  suspend fun write(snapshot: String)
}

/**
 * TODO(#58): Temporary snapshot persistence for the in-memory repository. Delete this once
 *   FHIREngine-backed persistence is implemented; that should make these platform-specific stores
 *   unnecessary.
 */
expect object PlatformRepositorySnapshotStore : RepositorySnapshotStore {
  override suspend fun read(): String?

  override suspend fun write(snapshot: String)
}
