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
import dev.ohs.player.generated.state.GroupMemberState
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupRepositoryTest {
  @Test
  fun buildGroupProfileUiState_usesRelationshipFreePatientAsHead() {
    val result =
      GroupRepository.buildGroupProfileUiState(
        groupHeader =
          GroupHeaderState(groupId = "group-1", groupName = "Doe Household", memberCount = "3"),
        extractedMembers =
          listOf(
            GroupMemberState(
              memberId = "head-1",
              memberGivenName = "Jane",
              memberFamilyName = "Doe",
            ),
            GroupMemberState(
              memberId = "member-1",
              memberGivenName = "Junior",
              memberFamilyName = "Doe",
              relationshipCode = "child",
            ),
            GroupMemberState(
              memberId = "member-2",
              memberGivenName = "John",
              memberFamilyName = "Doe",
              relationshipCode = "spouse",
            ),
          ),
      )

    assertEquals("Jane", result.groupHeader?.headGivenName)
    assertEquals("Doe", result.groupHeader?.headFamilyName)
    assertEquals(
      listOf("head-1", "member-1", "member-2"),
      result.members.mapNotNull { it.memberId },
    )
    assertEquals(listOf(null, "child", "spouse"), result.members.map { it.relationshipCode })
  }

  @Test
  fun buildGroupProfileUiState_fallsBackToFirstMemberWhenEveryMemberHasRelationship() {
    val result =
      GroupRepository.buildGroupProfileUiState(
        groupHeader =
          GroupHeaderState(
            groupId = "group-1",
            groupName = "Diallo Household",
            memberCount = "2",
            headGivenName = "Existing",
            headFamilyName = "Head",
          ),
        extractedMembers =
          listOf(
            GroupMemberState(
              memberId = "member-1",
              memberGivenName = "Amina",
              memberFamilyName = "Diallo",
              relationshipCode = "spouse",
            ),
            GroupMemberState(
              memberId = "member-2",
              memberGivenName = "Yuki",
              memberFamilyName = "Diallo",
              relationshipCode = "child",
            ),
          ),
      )

    assertEquals("Amina", result.groupHeader?.headGivenName)
    assertEquals("Diallo", result.groupHeader?.headFamilyName)
    assertEquals(listOf("member-1", "member-2"), result.members.mapNotNull { it.memberId })
    assertEquals(listOf("spouse", "child"), result.members.mapNotNull { it.relationshipCode })
  }

  @Test
  fun buildGroupProfileUiState_keepsHeadNavigableForSingleMemberHousehold() {
    val result =
      GroupRepository.buildGroupProfileUiState(
        groupHeader =
          GroupHeaderState(groupId = "group-1", groupName = "Solo Household", memberCount = "1"),
        extractedMembers =
          listOf(
            GroupMemberState(
              memberId = "head-1",
              memberGivenName = "Jane",
              memberFamilyName = "Doe",
            )
          ),
      )

    assertEquals("Jane", result.groupHeader?.headGivenName)
    assertEquals("Doe", result.groupHeader?.headFamilyName)
    assertEquals(listOf("head-1"), result.members.mapNotNull { it.memberId })
  }
}
