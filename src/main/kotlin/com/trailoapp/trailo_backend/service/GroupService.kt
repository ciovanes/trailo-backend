package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.domain.enum.GroupRoles
import com.trailoapp.trailo_backend.domain.enum.MembershipStatus
import com.trailoapp.trailo_backend.domain.social.GroupEntity
import com.trailoapp.trailo_backend.domain.social.UserGroupEntity
import com.trailoapp.trailo_backend.dto.group.request.CreateGroupRequest
import com.trailoapp.trailo_backend.dto.group.request.UpdateGroupRequest
import com.trailoapp.trailo_backend.dto.group.response.GroupMemberResponse
import com.trailoapp.trailo_backend.dto.group.response.PrivateGroupMembersResponse
import com.trailoapp.trailo_backend.dto.group.response.PublicGroupMembersResponse
import com.trailoapp.trailo_backend.dto.user.response.UserResponse
import com.trailoapp.trailo_backend.exception.definitions.BusinessRuleException
import com.trailoapp.trailo_backend.exception.definitions.DuplicateResourceException
import com.trailoapp.trailo_backend.exception.definitions.PermissionDeniedException
import com.trailoapp.trailo_backend.exception.definitions.ResourceNotFoundException
import com.trailoapp.trailo_backend.exception.definitions.SelfActionException
import com.trailoapp.trailo_backend.repository.GroupRepository
import com.trailoapp.trailo_backend.repository.UserGroupRepository
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val userGroupRepository: UserGroupRepository
) {

    // ===== GROUP MANAGEMENT =====

    @Transactional
    fun createGroup(groupRequest: CreateGroupRequest, owner: UserEntity): GroupEntity {
        if (groupRepository.existsByName(groupRequest.name)) {
            throw DuplicateResourceException("Group", "name", groupRequest.name)
        }

        val group = groupRepository.save(
            GroupEntity(
                name = groupRequest.name,
                description = groupRequest.description,
                isPrivate = groupRequest.isPrivate,
                imageUrl = groupRequest.imageUrl
            )
        )

        userGroupRepository.save(
            UserGroupEntity(
                group = group,
                user = owner,
                status = MembershipStatus.ACCEPTED,
                role = GroupRoles.LEADER,
                invitedBy = owner.uuid
            )
        )

        return group
    }

    fun findGroupByUuid(uuid: UUID): GroupEntity? {
        return groupRepository.findByIdOrNull(uuid)
    }

    @Transactional
    fun updateGroup(userId: UUID, groupId: UUID, request: UpdateGroupRequest): GroupEntity {
        val group = getGroupByUuid(groupId)

        verifyUserHasPermissions(userId, groupId)

        request.apply {
            description?.let { group.description = it }
            isPrivate?.let { group.isPrivate = it }
            imageUrl?.let { group.imageUrl = it }
        }

        return groupRepository.save(group)
    }

    @Transactional
    fun deleteGroup(groupId: UUID, userId: UUID) {
        val group = getGroupByUuid(groupId)

        verifyUserHasPermissions(userId, groupId)

        groupRepository.delete(group)
    }

    // ===== GROUP LISTING AND SEARCH =====

    fun getAllGroups(pageable: Pageable): Page<GroupEntity> {
        return groupRepository.findAll(pageable)
    }

    fun searchByName(query: String, pageable: Pageable): Page<GroupEntity> {
        return groupRepository.searchByNameContainingIgnoreCase(query, pageable)
    }

    fun searchByIsPrivate(isPrivate: Boolean, pageable: Pageable): Page<GroupEntity> {
        return groupRepository.searchByIsPrivate(isPrivate, pageable)
    }

    fun getMyGroups(userId: UUID, pageable: Pageable): Page<GroupEntity> {
        return userGroupRepository.findMyGroups(userId, pageable)
    }

    fun getFavoriteGroups(userId: UUID, pageable: Pageable): Page<GroupEntity> {
        return userGroupRepository.findFavoriteGroups(userId, pageable)
    }

    // ===== GROUP MEMBERSHIP =====

    @Transactional
    fun joinGroup(user: UserEntity, group: UUID): UserGroupEntity {
        val group = getGroupByUuid(group)

        userGroupRepository.findByGroup_UuidAndUser_Uuid(group.uuid, user.uuid)
            .ifPresent { throw BusinessRuleException("User is already a member of this group") }

        return userGroupRepository.save(
            UserGroupEntity(
                group = group,
                user = user,
                status = if (group.isPrivate) MembershipStatus.PENDING else MembershipStatus.ACCEPTED,
                role = GroupRoles.MEMBER,
                invitedBy = user.uuid,
                isFavorite = false
            )
        )
    }

    @Transactional
    fun leaveGroup(userId: UUID, groupId: UUID) {
        val membership = getUserMembershipOrThrow(userId, groupId)

        if (membership.role == GroupRoles.LEADER) {
            throw BusinessRuleException("Leader cannot leave the group")
        }

        userGroupRepository.delete(membership)
    }

    fun kickMember(adminId: UUID, groupId: UUID, userId: UUID) {
        if (adminId == userId) {
            throw SelfActionException("kick from group")
        }

        verifyUserHasPermissions(adminId, groupId)

        val membership = getUserMembershipOrThrow(userId, groupId, "User is not a member of this group")

        val adminMembership = getUserMembershipOrThrow(adminId, groupId)

        // check if the user has enough permissions to kick the user
        if (adminMembership.role == GroupRoles.CO_LEADER &&
            (membership.role == GroupRoles.LEADER || membership.role == GroupRoles.CO_LEADER)) {
            throw PermissionDeniedException("kick from group","leader or co-leader")
        }

        userGroupRepository.delete(membership)
    }


    // ===== ROLE AND PERMISSION MANAGEMENT =====

    @Transactional
    fun updateMemberRole(adminId: UUID, groupId: UUID, userId: UUID, newRole: GroupRoles) {
        val adminMembership = getUserMembershipOrThrow(adminId, groupId)

        if (adminMembership.role != GroupRoles.LEADER) {
            throw PermissionDeniedException("update role", "member")
        }

        val membership = getUserMembershipOrThrow(userId, groupId, "User is not a member of this group")

        if (membership.role == GroupRoles.LEADER) {
            throw BusinessRuleException("Can't change the role of the leader")
        }

        if (newRole == GroupRoles.LEADER) {
            adminMembership.role = GroupRoles.CO_LEADER
            userGroupRepository.save(adminMembership)
        }

        membership.role = newRole
        userGroupRepository.save(membership)
    }

    fun updateMembershipRequest(adminId: UUID, groupId: UUID, userId: UUID, newStatus: MembershipStatus) {
        verifyUserHasPermissions(adminId, groupId)

        val userGroup = getUserMembershipOrThrow(userId, groupId, "Membership not found")

        if (userGroup.status != MembershipStatus.PENDING) {
            throw BusinessRuleException("The membership status cannot be changed")
        }

        userGroup.status = newStatus
        userGroupRepository.save(userGroup)
    }

    fun getPendingRequests(userId: UUID, groupId: UUID, pageable: Pageable): Page<UserEntity> {
        verifyUserHasPermissions(userId, groupId)

        return userGroupRepository.getPendingRequests(groupId, pageable)
    }

    // ===== GROUP MEMBERSHIP LISTING =====

    fun getGroupMembers(userId: UUID, groupId: UUID, pageable: Pageable): GroupMemberResponse {
        val group = getGroupByUuid(groupId)
        val isMember = userGroupRepository.userIsMemberOfGroup(userId, groupId)
        val memberCount = userGroupRepository.countAcceptedMembersByGroupId(groupId)

        return if (group.isPrivate && !isMember) {
            PrivateGroupMembersResponse(totalElements = memberCount)
        } else {
            val members = userGroupRepository.findMembersByGroupId(groupId, pageable)

            PublicGroupMembersResponse(
                members = members.content.map { UserResponse.fromUser(it) },
                pageNumber = members.number,
                pageSize = members.size,
                totalElements = members.totalElements,
                totalPages = members.totalPages,
                isLast = members.isLast
            )
        }
    }

    // ===== FAVORITE GROUPS =====

    @Transactional
    fun toggleFavorite(user: UUID, group: UUID) {
        val userGroup = getUserMembershipOrThrow(user, group, "You are not a member of this group")

        userGroup.isFavorite = !userGroup.isFavorite
        userGroupRepository.save(userGroup)
    }

    fun checkIsFavorite(user: UUID, group: UUID): Boolean {
        return getUserMembershipOrThrow(user, group, "You are not a member of this group")
            .isFavorite
    }

    // ===== UTILITY METHODS =====

    /**
     * Find a GroupEntity by its UUID.
     *
     * @param uuid The UUID of the group to find.
     * @return The GroupEntity with the specified UUID.
     * @throws ResourceNotFoundException if the group is not found.
     */
    private fun getGroupByUuid(uuid: UUID): GroupEntity {
        return groupRepository.findById(uuid)
            .orElseThrow { ResourceNotFoundException("Group", uuid) }
    }

    /**
     * Verifies that the user is a member of the group and has the manage permissions.
     *
     * @param userId The UUID of the user to check.
     * @param groupId The UUID of the group to check.
     * @throws PermissionDeniedException if the user is not a member of the group or does not have enough permissions.
     */
    private fun verifyUserHasPermissions(userId: UUID, groupId: UUID){
        if (!userGroupRepository.userHavePermissions(userId, groupId)) {
            throw PermissionDeniedException("manage", "group", groupId)
        }
    }

    /**
     * Throws a ResourceNotFoundException if the user is not a member of the group.
     *
     * @param userId The UUID of the user to check.
     * @param groupId The UUID of the group to check.
     * @param errorMessage The error message to include in the exception if the user is not a member of the group.
     * @return The UserGroupEntity representing the user's membership in the group.
     * @throws ResourceNotFoundException if the user is not a member of the group.
     */
    private fun getUserMembershipOrThrow(
        userId: UUID,
        groupId: UUID,
        errorMessage: String = "User-group relationship not found"
    ): UserGroupEntity {
        return userGroupRepository.findByGroup_UuidAndUser_Uuid(groupId, userId)
            .orElseThrow { ResourceNotFoundException(errorMessage, "$userId, $groupId") }
    }
}