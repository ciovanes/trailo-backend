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

    /**
     * Creates a new group with the provided [CreateGroupRequest].
     *
     * @param groupRequest The [CreateGroupRequest] containing the fields of the new group.
     * @param owner The [UserEntity] representing the owner of the group.
     * @return The new [GroupEntity] created with the provided data.
     */
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

    /**
     * Finds a group with a specified UUID.
     *
     * @param uuid The [UUID] of the group to find.
     * @return The [GroupEntity] with the specified UUID, or null if not found.
     */
    fun findGroupByUuid(uuid: UUID): GroupEntity? {
        return groupRepository.findByIdOrNull(uuid)
    }

    /**
     * Update the group with the specified UUID with the provided [UpdateGroupRequest].
     *
     * @param userId The [UUID] of the user making the request.
     * @param groupId The [UUID] of the group to update.
     * @param request The [UpdateGroupRequest] containing the fields to update.
     * @return The updated [GroupEntity]
     */
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

    /**
     * Deletes the group with the specified UUID.
     *
     * @param groupId The [UUID] of the group to delete.
     * @param userId The [UUID] of the user making the request.
     */
    @Transactional
    fun deleteGroup(groupId: UUID, userId: UUID) {
        val group = getGroupByUuid(groupId)

        verifyUserHasPermissions(userId, groupId)

        groupRepository.delete(group)
    }

    // ===== GROUP LISTING AND SEARCH =====

    /**
     * Finds all the groups in the database.
     *
     * @param pageable The [Pageable] object containing pagination and sorting information.
     * @return A [Page] of [GroupEntity] objects representing all the groups in the database.
     */
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

    /**
     * Joins a group with the specified [UUID]
     *
     * @param user The [UserEntity] that wants to join the group.
     * @param groupId The [UUID] of the group to join.
     * @return The [UserGroupEntity] representing the membership.
     */
    @Transactional
    fun joinGroup(user: UserEntity, groupId: UUID): UserGroupEntity {
        val group = getGroupByUuid(groupId)

        userGroupRepository.findByGroup_UuidAndUser_Uuid(group.uuid, user.uuid)
            ?.let{ throw BusinessRuleException("User is already a member of this group") }

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

    /**
     * Leave a group with the specified [UUID].
     *
     * @param userId The [UUID] of the user that wants to leave the group.
     * @param groupId The [UUID] of the group to leave.
     */
    @Transactional
    fun leaveGroup(userId: UUID, groupId: UUID) {
        val membership = getUserMembershipOrThrow(userId, groupId)

        if (membership.role == GroupRoles.LEADER) {
            throw BusinessRuleException("Leader cannot leave the group")
        }

        userGroupRepository.delete(membership)
    }

    /**
     * Kick a member from a group.
     *
     * @param adminId The [UUID] of the user that have enough permissions to kick the member.
     * @param groupId The [UUID] of the group to kick the member from.
     * @param userId The [UUID] of the member to kick.
     */
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

    /**
     * Update the role of a member in a group.
     *
     * @param adminId The [UUID] of the user that have enough permissions to update the role.
     * @param groupId The [UUID] of the group to update the role in.
     * @param userId The [UUID] of the member to update the role for.
     */
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

    /**
     * Update the membership status of a membership.
     *
     * @param adminId The [UUID] of the user that have enough permissions to update the membership status.
     * @param groupId The [UUID] of the group to update the membership status.
     * @param userId The [UUID] of the member to update the membership status for.
     * @param newStatus The new [MembershipStatus].
     */
    fun updateMembershipRequest(adminId: UUID, groupId: UUID, userId: UUID, newStatus: MembershipStatus) {
        verifyUserHasPermissions(adminId, groupId)

        val userGroup = getUserMembershipOrThrow(userId, groupId, "Membership not found")

        if (userGroup.status != MembershipStatus.PENDING) {
            throw BusinessRuleException("The membership status cannot be changed")
        }

        if (newStatus == MembershipStatus.REJECTED) {
            userGroupRepository.delete(userGroup)
            return
        }

        userGroup.status = newStatus
        userGroupRepository.save(userGroup)
    }

    /**
     * Get all the pending membership requests for a group.
     *
     * @param adminId The [UUID] of the user that have enough permission to view the pending membership requests.
     * @param groupId The [UUID] of the group to view the pending memberships.
     * @param pageable The [Pageable] object containing pagination and sorting information.
     * @return A [Page] of [UserEntity] objects representing the pending membership requests.
     */
    fun getPendingRequests(adminId: UUID, groupId: UUID, pageable: Pageable): Page<UserEntity> {
        verifyUserHasPermissions(adminId, groupId)

        return userGroupRepository.getPendingRequests(groupId, pageable)
    }

    // ===== GROUP MEMBERSHIP LISTING =====

    /**
     * Get all the members of a group.
     *
     * @param userId The [UUID] of the user that wants to view the members.
     * @param groupId The [UUID] of the group to view the members.
     * @param pageable The [Pageable] object containing pagination and sorting information.
     * @return A [GroupMemberResponse] object containing the members and pagination information.
     */
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

    /**
     * Toggle the favorite status of a group.
     *
     * @param userId The [UUID] of the user that wants to change the favorite status.
     * @param groupId The [UUID] of the group to change the status from.
     */
    @Transactional
    fun toggleFavorite(userId: UUID, groupId: UUID) {
        val userGroup = getUserMembershipOrThrow(userId, groupId, "You are not a member of this group")

        userGroup.isFavorite = !userGroup.isFavorite
        userGroupRepository.save(userGroup)
    }

    /**
     * Check if a user has a group in their favorite list.
     *
     * @param userId The [UUID] of the user to check.
     * @param groupId The [UUID] of the group to check.
     */
    fun checkIsFavorite(userId: UUID, groupId: UUID): Boolean {
        return getUserMembershipOrThrow(userId, groupId, "You are not a member of this group")
            .isFavorite
    }

    // ===== UTILITY METHODS =====

    /**
     * Find a [GroupEntity] by its UUID.
     *
     * @param uuid The [UUID] of the group to find.
     * @return The [GroupEntity] with the specified UUID.
     * @throws ResourceNotFoundException if the group is not found.
     */
    private fun getGroupByUuid(uuid: UUID): GroupEntity {
        return groupRepository.findById(uuid)
            .orElseThrow { ResourceNotFoundException("Group", uuid) }
    }

    /**
     * Verifies that the user is a member of the group and has the manage permissions.
     *
     * @param userId The [UUID] of the user to check.
     * @param groupId The [UUID] of the group to check.
     * @throws PermissionDeniedException if the user is not a member of the group or does not have enough permissions.
     */
    private fun verifyUserHasPermissions(userId: UUID, groupId: UUID){
        if (!userGroupRepository.userHavePermissions(userId, groupId)) {
            throw PermissionDeniedException("manage", "group", groupId)
        }
    }

    /**
     * Throws a [ResourceNotFoundException] if the user is not a member of the group.
     *
     * @param userId The [UUID] of the user to check.
     * @param groupId The [UUID] of the group to check.
     * @param errorMessage The error message to include in the exception if the user is not a member of the group.
     * @return The [UserGroupEntity] representing the user's membership in the group.
     * @throws ResourceNotFoundException if the user is not a member of the group.
     */
    private fun getUserMembershipOrThrow(
        userId: UUID,
        groupId: UUID,
        errorMessage: String = "User-group relationship not found"
    ): UserGroupEntity {
        return userGroupRepository.findByGroup_UuidAndUser_Uuid(groupId, userId)
            ?: throw ResourceNotFoundException(errorMessage, "$userId, $groupId")
    }
}