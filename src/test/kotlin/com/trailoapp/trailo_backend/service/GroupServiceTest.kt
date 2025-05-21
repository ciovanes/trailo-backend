package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.core.GroupEntity
import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.domain.enum.social.GroupRoles
import com.trailoapp.trailo_backend.domain.enum.social.MembershipStatus
import com.trailoapp.trailo_backend.domain.social.UserGroupEntity
import com.trailoapp.trailo_backend.dto.group.request.CreateGroupRequest
import com.trailoapp.trailo_backend.dto.group.request.UpdateGroupRequest
import com.trailoapp.trailo_backend.dto.group.response.PrivateGroupMembersResponse
import com.trailoapp.trailo_backend.dto.group.response.PublicGroupMembersResponse
import com.trailoapp.trailo_backend.exception.definitions.BusinessRuleException
import com.trailoapp.trailo_backend.exception.definitions.DuplicateResourceException
import com.trailoapp.trailo_backend.exception.definitions.PermissionDeniedException
import com.trailoapp.trailo_backend.exception.definitions.SelfActionException
import com.trailoapp.trailo_backend.repository.GroupRepository
import com.trailoapp.trailo_backend.repository.UserGroupRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.*
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class GroupServiceTest {

    @Mock
    private lateinit var groupRepository: GroupRepository

    @Mock
    private lateinit var userGroupRepository: UserGroupRepository

    private lateinit var groupService: GroupService

    // Test data
    private lateinit var testUser: UserEntity
    private lateinit var testGroup: GroupEntity
    private lateinit var testUserGroup: UserGroupEntity

    private val testUserId = UUID.randomUUID()
    private val testGroupId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        groupService = GroupService(groupRepository, userGroupRepository)

        testUser = UserEntity(
            uuid = testUserId,
            email = "test@trailo-app.com",
            username = "testuser",
            cognitoId = "test-cognito-id"
        )

        testGroup = GroupEntity(
            uuid = testGroupId,
            name = "testgroup",
            isPrivate = false
        )

        testUserGroup = UserGroupEntity(
            uuid = UUID.randomUUID(),
            group = testGroup,
            user = testUser,
            status = MembershipStatus.ACCEPTED,
            role = GroupRoles.LEADER,
            invitedBy = testUserId
        )
    }

    @Test
    fun `createGroup creates and returns a new group when data is valid`() {
        val createGroupRequest = CreateGroupRequest(
            name = "newgroup",
            isPrivate = false
        )

        whenever(groupRepository.existsByName(createGroupRequest.name)).thenReturn(false)

        whenever(groupRepository.save(any(GroupEntity::class.java))).thenAnswer {
            val savedGroup = it.getArgument<GroupEntity>(0)
            savedGroup.copy(uuid = UUID.randomUUID())
        }

        whenever(userGroupRepository.save(any(UserGroupEntity::class.java))).thenAnswer {
            val savedUserGroup = it.getArgument<UserGroupEntity>(0)
            savedUserGroup.copy(uuid = UUID.randomUUID())
        }

        val result = groupService.createGroup(createGroupRequest, testUser)

        assertNotNull(result)
        assertNotNull(result.uuid)
        assertEquals(createGroupRequest.name, result.name)
        assertEquals(createGroupRequest.isPrivate, result.isPrivate)

        verify(groupRepository).existsByName(createGroupRequest.name)
        verify(groupRepository).save(any(GroupEntity::class.java))
        verify(userGroupRepository).save(any(UserGroupEntity::class.java))
    }

    @Test
    fun `createGroup throws DuplicateResourceException when group name already exists`() {
        val createGroupRequest = CreateGroupRequest(
            name = testGroup.name,
            isPrivate = false
        )

        whenever(groupRepository.existsByName(createGroupRequest.name)).thenReturn(true)

        val exception = assertThrows(DuplicateResourceException::class.java) {
            groupService.createGroup(createGroupRequest, testUser)
        }

        assertTrue(exception.message?.contains("name") == true)

        verify(groupRepository).existsByName(createGroupRequest.name)
    }

    @Test
    fun `findGroupById returns group when group exists`() {
        whenever(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup))

        val result = groupService.findGroupByUuid(testGroupId)

        assertNotNull(result)
        assertEquals(testGroupId, result?.uuid)
        assertEquals(testGroup.name, result?.name)
        assertEquals(testGroup.isPrivate, result?.isPrivate)

        verify(groupRepository).findById(testGroupId)
    }

    @Test
    fun `findGroupById returns null when group does not exists`() {
        whenever(groupRepository.findById(testGroupId)).thenReturn(Optional.empty())

        val result = groupService.findGroupByUuid(testGroupId)

        assertNull(result)

        verify(groupRepository).findById(testGroupId)
    }

    @Test
    fun `updateGroup updates and returns group when user has permissions`() {
        val updateGroupRequest = UpdateGroupRequest(
            description = "Updated description",
            isPrivate = true
        )

        whenever(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup))
        whenever(userGroupRepository.userHavePermissions(testUserId, testGroupId)).thenReturn(true)
        whenever(groupRepository.save(any(GroupEntity::class.java))).thenAnswer {
            it.getArgument<GroupEntity>(0)
        }

        val result = groupService.updateGroup(testUserId, testGroupId, updateGroupRequest)

        assertNotNull(result)
        assertEquals(testGroupId, result.uuid)
        assertEquals(testGroup.name, result.name)
        assertEquals(updateGroupRequest.description, result.description)
        assertTrue(result.isPrivate)

        verify(groupRepository).findById(testGroupId)
        verify(userGroupRepository).userHavePermissions(testUserId, testGroupId)
        verify(groupRepository).save(any(GroupEntity::class.java))
    }

    @Test
    fun `updateGroup throws PermissionDeniedException when user does not have permissions`() {
        val updateGroupRequest = UpdateGroupRequest(
            description = "Updated description",
            isPrivate = true
        )

        whenever(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup))
        whenever(userGroupRepository.userHavePermissions(testUserId, testGroupId)).thenReturn(false)

        assertThrows(PermissionDeniedException::class.java) {
            groupService.updateGroup(testUserId, testGroupId, updateGroupRequest)
        }

        verify(groupRepository).findById(testGroupId)
        verify(userGroupRepository).userHavePermissions(testUserId, testGroupId)
    }

    @Test
    fun `deleteGroup deletes group when user has permissions`() {
        whenever(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup))
        whenever(userGroupRepository.userHavePermissions(testUserId, testGroupId)).thenReturn(true)

        groupService.deleteGroup(testGroupId, testUserId)

        verify(groupRepository).findById(testGroupId)
        verify(userGroupRepository).userHavePermissions(testUserId, testGroupId)
        verify(groupRepository).delete(testGroup)
    }

    @Test
    fun `joinGroup allows user to join group when data is valid`() {
        whenever(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup))
        whenever(userGroupRepository.findByGroup_UuidAndUser_UuidAccepted(testGroupId, testUserId))
            .thenReturn(null)
        whenever(userGroupRepository.save(any(UserGroupEntity::class.java))).thenAnswer {
            val savedUserGroup = it.getArgument<UserGroupEntity>(0)
            savedUserGroup.copy(uuid = UUID.randomUUID())
        }

        val result = groupService.joinGroup(testUser, testGroupId)

        assertNotNull(result)
        assertEquals(testGroup, result.group)
        assertEquals(testUser, result.user)
        assertEquals(MembershipStatus.ACCEPTED, result.status)
        assertEquals(GroupRoles.MEMBER, result.role)

        verify(groupRepository).findById(testGroupId)
        verify(userGroupRepository).findByGroup_UuidAndUser_UuidAccepted(testGroupId, testUserId)
        verify(userGroupRepository).save(any(UserGroupEntity::class.java))
    }

    @Test
    fun `joinGroup throw  BusinessRuleException when user already joined group`() {
        whenever(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup))
        whenever(userGroupRepository.findByGroup_UuidAndUser_UuidAccepted(testGroupId, testUserId))
            .thenReturn(testUserGroup)

        assertThrows(BusinessRuleException::class.java) {
            groupService.joinGroup(testUser, testGroupId)
        }

        verify(groupRepository).findById(testGroupId)
        verify(userGroupRepository).findByGroup_UuidAndUser_UuidAccepted(testGroupId, testUserId)
    }

    @Test
    fun `leaveGroup allows user to leave group when user is not leader`() {
        val memberUserGroup = testUserGroup.copy(role = GroupRoles.MEMBER)

        whenever(userGroupRepository.findByGroup_UuidAndUser_Uuid(testGroupId, testUserId))
            .thenReturn(memberUserGroup)

        groupService.leaveGroup(testUserId, testGroupId)

        verify(userGroupRepository).findByGroup_UuidAndUser_Uuid(testGroupId, testUserId)
        verify(userGroupRepository).delete(memberUserGroup)
    }

    @Test
    fun `leaveGroup throws BusinessRuleException when user is leader`() {
        whenever(userGroupRepository.findByGroup_UuidAndUser_Uuid(testGroupId, testUserId))
            .thenReturn(testUserGroup)

        assertThrows(BusinessRuleException::class.java) {
            groupService.leaveGroup(testUserId, testGroupId)
        }

        verify(userGroupRepository).findByGroup_UuidAndUser_Uuid(testGroupId, testUserId)
    }

    @Test
    fun `kickMember allows admin to kick member from group`() {
        val adminId = UUID.randomUUID()
        val memberId = UUID.randomUUID()

        val adminUserGroup = testUserGroup.copy(uuid = UUID.randomUUID(), user = testUser.copy(uuid = adminId))
        val memberUserGroup = testUserGroup.copy(uuid = UUID.randomUUID(), user = testUser.copy(uuid = memberId), role = GroupRoles.MEMBER)

        whenever(userGroupRepository.userHavePermissions(adminId, testGroupId)).thenReturn(true)
        whenever(userGroupRepository.findByGroup_UuidAndUser_Uuid(testGroupId, memberId)).thenReturn(memberUserGroup)
        whenever(userGroupRepository.findByGroup_UuidAndUser_Uuid(testGroupId, adminId)).thenReturn(adminUserGroup)

        groupService.kickMember(adminId, testGroupId, memberId)

        verify(userGroupRepository).userHavePermissions(adminId, testGroupId)
        verify(userGroupRepository).findByGroup_UuidAndUser_Uuid(testGroupId, memberId)
        verify(userGroupRepository).findByGroup_UuidAndUser_Uuid(testGroupId, adminId)
        verify(userGroupRepository).delete(memberUserGroup)
    }

    @Test
    fun `kickMember throws SelfActionException when user tries to kick himself`() {
        assertThrows(SelfActionException::class.java) {
            groupService.kickMember(testUserId, testGroupId, testUserId)
        }
    }

    @Test
    fun `kickMember throws PermissionDeniedException when user does not have permissions`() {
        val leaderId = UUID.randomUUID()
        val coleaderId = UUID.randomUUID()

        val leaderUserGroup = testUserGroup.copy(uuid = UUID.randomUUID(), user = testUser.copy(uuid = leaderId),
            role = GroupRoles.LEADER)
        val coleaderUserGroup = testUserGroup.copy(uuid = UUID.randomUUID(), user = testUser.copy(uuid = coleaderId),
            role = GroupRoles.CO_LEADER)

        whenever(userGroupRepository.userHavePermissions(coleaderId, testGroupId)).thenReturn(true)
        whenever(userGroupRepository.findByGroup_UuidAndUser_Uuid(testGroupId, leaderId)).thenReturn(leaderUserGroup)
        whenever(userGroupRepository.findByGroup_UuidAndUser_Uuid(testGroupId, coleaderId)).thenReturn(coleaderUserGroup)

        assertThrows(PermissionDeniedException::class.java) {
            groupService.kickMember(coleaderId, testGroupId, leaderId)
        }
    }

    @Test
    fun `updateMemberRole allows admin to update member role`() {
        val memberId = UUID.randomUUID()
        val newRole = GroupRoles.ELDER

        val memberUserGroup = testUserGroup.copy(uuid = UUID.randomUUID(), user = testUser.copy(uuid = memberId),
            role = GroupRoles.MEMBER)

        whenever(userGroupRepository.findByGroup_UuidAndUser_Uuid(testGroupId, testUserId))
            .thenReturn(testUserGroup)
        whenever(userGroupRepository.findByGroup_UuidAndUser_Uuid(testGroupId, memberId))
            .thenReturn(memberUserGroup)
        whenever(userGroupRepository.save(any(UserGroupEntity::class.java))).thenAnswer {
            it.getArgument<UserGroupEntity>(0)
        }

        groupService.updateMemberRole(testUserId, testGroupId, memberId, newRole)

        assertEquals(GroupRoles.ELDER, memberUserGroup.role)

        verify(userGroupRepository).findByGroup_UuidAndUser_Uuid(testGroupId, testUserId)
        verify(userGroupRepository).findByGroup_UuidAndUser_Uuid(testGroupId, memberId)
        verify(userGroupRepository).save(any(UserGroupEntity::class.java))
    }

    @Test
    fun `updateMemberRole throws PermissionDeniedException when user does not have permissions`() {
        val memberId = UUID.randomUUID()
        val newRole = GroupRoles.ELDER

        val adminUserGroup = testUserGroup.copy(role = GroupRoles.CO_LEADER)

        whenever(userGroupRepository.findByGroup_UuidAndUser_Uuid(testGroupId, testUserId))
            .thenReturn(adminUserGroup)

        assertThrows(PermissionDeniedException::class.java) {
            groupService.updateMemberRole(testUserId, testGroupId, memberId, newRole)
        }

        verify(userGroupRepository).findByGroup_UuidAndUser_Uuid(testGroupId, testUserId)
    }

    @Test
    fun `updateMemberRole throws BusinessRuleException when leader tries to update his role`() {
        val memberId = UUID.randomUUID()
        val newRole = GroupRoles.ELDER

        val memberUserGroup = testUserGroup.copy(uuid = UUID.randomUUID(),
            user = testUser.copy(uuid = memberId), role = GroupRoles.LEADER)

        whenever(userGroupRepository.findByGroup_UuidAndUser_Uuid(testGroupId, testUserId))
            .thenReturn(testUserGroup)
        whenever(userGroupRepository.findByGroup_UuidAndUser_Uuid(testGroupId, memberId))
            .thenReturn(memberUserGroup)

        assertThrows(BusinessRuleException::class.java) {
            groupService.updateMemberRole(testUserId, testGroupId, memberId, newRole)
        }

        verify(userGroupRepository).findByGroup_UuidAndUser_Uuid(testGroupId, testUserId)
        verify(userGroupRepository).findByGroup_UuidAndUser_Uuid(testGroupId, memberId)
    }

    @Test
    fun `toggleFavorite changes favorite status of group`() {
        val userGroup = testUserGroup.copy(isFavorite = false)

        whenever(userGroupRepository.findByGroup_UuidAndUser_Uuid(testGroupId, testUserId))
            .thenReturn(userGroup)
        whenever(userGroupRepository.save(any(UserGroupEntity::class.java))).thenAnswer {
            it.getArgument<UserGroupEntity>(0)
        }

        groupService.toggleFavorite(testUserId, testGroupId)

        assertTrue(userGroup.isFavorite)

        verify(userGroupRepository).findByGroup_UuidAndUser_Uuid(testGroupId, testUserId)
        verify(userGroupRepository).save(any(UserGroupEntity::class.java))
    }

    @Test
    fun `getGroupMembers returns PrivateGroupMembersResponse when group is private and user is not member`() {
        val privateGroup = testGroup.copy(isPrivate = true)

        whenever(groupRepository.findById(testGroupId)).thenReturn(Optional.of(privateGroup))
        whenever(userGroupRepository.userIsMemberOfGroup(testUserId, testGroupId)).thenReturn(false)
        whenever(userGroupRepository.countAcceptedMembersByGroupId(testGroupId)).thenReturn(10)

        val pageable = PageRequest.of(0, 10)
        val result = groupService.getGroupMembers(testUserId, testGroupId, pageable)

        assertTrue(result is PrivateGroupMembersResponse)
        assertEquals(10, (result as PrivateGroupMembersResponse).totalElements)
        assertTrue(result.isPrivate)

        verify(groupRepository).findById(testGroupId)
        verify(userGroupRepository).userIsMemberOfGroup(testUserId, testGroupId)
        verify(userGroupRepository).countAcceptedMembersByGroupId(testGroupId)
    }

    @Test
    fun `getGroupMembers returns PublicGroupMembersResponse when group is public`() {
        whenever(groupRepository.findById(testGroupId)).thenReturn(Optional.of(testGroup))
        whenever(userGroupRepository.userIsMemberOfGroup(testUserId, testGroupId)).thenReturn(false)
        whenever(userGroupRepository.countAcceptedMembersByGroupId(testGroupId)).thenReturn(1)

        val pageable = PageRequest.of(0, 10)
        val membersPage = PageImpl(listOf(testUser), pageable, 1)
        whenever(userGroupRepository.findMembersByGroupId(testGroupId, pageable)).thenReturn(membersPage)

        val result = groupService.getGroupMembers(testUserId, testGroupId, pageable)

        assertTrue(result is PublicGroupMembersResponse)
        assertEquals(1, (result as PublicGroupMembersResponse).totalElements)
        assertFalse(result.isPrivate)

        verify(groupRepository).findById(testGroupId)
        verify(userGroupRepository).userIsMemberOfGroup(testUserId, testGroupId)
        verify(userGroupRepository).countAcceptedMembersByGroupId(testGroupId)
        verify(userGroupRepository).findMembersByGroupId(testGroupId, pageable)
    }
}