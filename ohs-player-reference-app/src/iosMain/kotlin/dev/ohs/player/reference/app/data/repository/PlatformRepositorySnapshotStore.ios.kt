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

import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataUsingEncoding
import platform.Foundation.writeToFile

private const val SNAPSHOT_FILE_NAME = "fhir-repository.json"

actual object PlatformRepositorySnapshotStore : RepositorySnapshotStore {
  override suspend fun read(): String? {
    val data = NSData.dataWithContentsOfFile(snapshotPath()) ?: return null
    return NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
  }

  override suspend fun write(snapshot: String) {
    val data = (snapshot as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
    data.writeToFile(snapshotPath(), true)
  }

  private fun snapshotPath(): String {
    val directory =
      (NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String) ?: NSTemporaryDirectory()
    return "$directory/$SNAPSHOT_FILE_NAME"
  }
}
