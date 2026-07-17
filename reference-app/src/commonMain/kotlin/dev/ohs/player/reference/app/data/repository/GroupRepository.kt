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

import dev.ohs.player.generated.state.GroupHeaderState
import dev.ohs.player.generated.state.GroupListState
import dev.ohs.player.generated.state.GroupMemberState
import dev.ohs.player.reference.app.data.AppDependencies
import dev.ohs.player.reference.app.data.Extraction.extractor
import dev.ohs.player.reference.app.data.datasource.groupListSearchResults
import dev.ohs.player.reference.app.data.datasource.groupProfileSearchResult
import dev.ohs.player.reference.app.feature.group.profile.GroupProfileUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

object GroupRepository {
  // FhirPathEvaluator is not concurrent-safe. limitedParallelism(1) serializes all extraction on a
  // single background thread without any explicit locking.
  private val extractorDispatcher = Dispatchers.Default.limitedParallelism(1)

  fun observeGroups(): Flow<List<GroupListState>> =
    AppDependencies.fhirRepository.revision.map { getGroups() }

  suspend fun getGroups(): List<GroupListState> =
    withContext(extractorDispatcher) {
      groupListSearchResults().flatMap { extractor.extract<GroupListState>(it) }
    }

  fun observeGroupProfile(groupId: String): Flow<GroupProfileUiState> =
    AppDependencies.fhirRepository.revision.map { getGroupProfile(groupId) }

  suspend fun getGroupProfile(groupId: String): GroupProfileUiState =
    withContext(extractorDispatcher) {
      val result = groupProfileSearchResult(groupId) ?: return@withContext GroupProfileUiState()
      buildGroupProfileUiState(
        groupHeader = extractor.extract<GroupHeaderState>(result).firstOrNull(),
        extractedMembers = extractor.extract<GroupMemberState>(result),
      )
    }

  internal fun buildGroupProfileUiState(
    groupHeader: GroupHeaderState?,
    extractedMembers: List<GroupMemberState>,
  ): GroupProfileUiState {
    if (extractedMembers.isEmpty()) {
      return GroupProfileUiState(groupHeader = groupHeader)
    }

    val headIndex = extractedMembers.indexOfFirst { it.relationshipCode.isNullOrBlank() }
    val resolvedHeadIndex = if (headIndex >= 0) headIndex else 0
    val head = extractedMembers[resolvedHeadIndex]

    return GroupProfileUiState(
      groupHeader =
        (groupHeader ?: GroupHeaderState()).copy(
          headGivenName = head.memberGivenName,
          headFamilyName = head.memberFamilyName,
        ),
      // Keep every Group.member navigable. In FHIR the head is still a legitimate member entity,
      // and hiding that row leaves single-person households with no path into the patient profile.
      members = extractedMembers,
    )
  }
}
