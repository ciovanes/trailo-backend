package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.domain.enum.GroupRoles
import com.trailoapp.trailo_backend.domain.enum.MembershipStatus
import com.trailoapp.trailo_backend.domain.social.GroupEntity
import com.trailoapp.trailo_backend.domain.social.UserGroupEntity
import com.trailoapp.trailo_backend.dto.common.response.PageResponse
import com.trailoapp.trailo_backend.dto.group.request.CreateGroupRequest
import com.trailoapp.trailo_backend.dto.group.response.GroupMemberResponse
import com.trailoapp.trailo_backend.dto.group.response.PrivateGroupMembersResponse
import com.trailoapp.trailo_backend.dto.group.response.PublicGroupMembersResponse
import com.trailoapp.trailo_backend.dto.user.response.UserResponse
import com.trailoapp.trailo_backend.repository.GroupRepository
import com.trailoapp.trailo_backend.repository.UserGroupRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
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

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun createGroup(groupRequest: CreateGroupRequest, owner: UserEntity): GroupEntity {
        if (groupRepository.existsByName(groupRequest.name)) {
            throw IllegalArgumentException("Group already exists: ${groupRequest.name}")
        }

        val group = GroupEntity(
            name = groupRequest.name,
            description = groupRequest.description,
            isPrivate = groupRequest.isPrivate,
            imageUrl = groupRequest.imageUrl
        ).let { groupRepository.save(it) }

        // Create a user_group entry
        UserGroupEntity(
            group = group,
            user = owner,
            status = MembershipStatus.ACCEPTED,
            role = GroupRoles.LEADER,
            invitedBy = owner.uuid
        ).also { userGroupRepository.save(it) }

        return group
    }

    fun getAllGroups(pageable: Pageable): Page<GroupEntity> {
        return groupRepository.findAll(pageable)
    }

    fun findGroupByUuid(uuid: UUID): GroupEntity? {
        return groupRepository.findByIdOrNull(uuid)
    }

    fun searchByName(query: String, pageable: Pageable): Page<GroupEntity> {
        return groupRepository.searchByNameContainingIgnoreCase(query, pageable)
    }

    fun searchByIsPrivate(isPrivate: Boolean, pageable: Pageable): Page<GroupEntity> {
        return groupRepository.searchByIsPrivate(isPrivate, pageable)
    }

    @Transactional
    fun deleteGroup(groupId: UUID, userId: UUID) {
        groupRepository.findById(groupId)
            .orElseThrow { Exception("Group not found") } // TODO: Create GroupNotFoundException
            .also { group ->
                // Check user permissions
                if (!userGroupRepository.userHavePermissions(userId, group.uuid)) {
                    throw Exception("User does not have permissions to delete this group") // TODO: Create UnauthorizedOperationException
                }
                groupRepository.delete(group)
            }
    }

    @Transactional
    fun joinGroup(user: UserEntity, group: UUID): UserGroupEntity {

        val group = groupRepository.findById(group)
            .orElseThrow{ Exception("Group not found") }

        val userGroupStatus = when {
            group.isPrivate -> MembershipStatus.PENDING
            else -> MembershipStatus.ACCEPTED
        }

        val userGroup = UserGroupEntity(
            group = group,
            user = user,
            status = userGroupStatus,
            role = GroupRoles.MEMBER,
            invitedBy = user.uuid,
            isFavorite = false
        )

        return userGroupRepository.save(userGroup)
    }

    @Transactional
    fun toggleFavorite(user: UUID, group: UUID) {
        val userGroup = userGroupRepository.findByGroup_UuidAndUser_Uuid(group, user)
            .orElseThrow { Exception("UserGroup not found") }

        userGroup.isFavorite = !userGroup.isFavorite
        userGroupRepository.save(userGroup)
    }

    fun checkIsFavorite(user: UUID, group: UUID): Boolean {
        return userGroupRepository.findByGroup_UuidAndUser_Uuid(group, user)
            .orElseThrow { Exception("UserGroup not found") } // TODO: ResourceNotFoundException
            .isFavorite
    }

    fun getPendingRequests(userId: UUID, groupId: UUID, pageable: Pageable): Page<UserEntity> {
        val havePermissions = userGroupRepository.userHavePermissions(userId, groupId)

        if (havePermissions) {
            return userGroupRepository.getPendingRequests(groupId, pageable)
        }

        throw Exception("User does not have permissions to view pending requests for this group") // TODO: PermissionDeniedException
    }

    fun getMyGroups(userId: UUID, pageable: Pageable): Page<GroupEntity> {
        return userGroupRepository.findMyGroups(userId, pageable)
    }

    fun getGroupMembers(userId: UUID, groupId: UUID, pageable: Pageable): GroupMemberResponse {

        val group = groupRepository.findById(groupId)
            .orElseThrow { Exception("Group not found") }

        val isMember = userGroupRepository.userIsMemberOfGroup(userId, groupId)

        val memberCount = userGroupRepository.countAcceptedMembersByGroupId(groupId)

        return if (group.isPrivate && !isMember) {
            PrivateGroupMembersResponse(
                totalElements = memberCount
            )

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

    fun updateMembershipRequest(adminId: UUID, groupId: UUID, userId: UUID, newStatus: MembershipStatus) {
        val havePermissions = userGroupRepository.userHavePermissions(adminId, groupId)

        if (havePermissions) {
            val userGroup = userGroupRepository.findByGroup_UuidAndUser_Uuid(groupId, userId)
                .orElseThrow { Exception("UserGroup not found") }

            if (userGroup.status == MembershipStatus.PENDING) {
                userGroup.status = newStatus
                userGroupRepository.save(userGroup)
            } else {
                throw Exception("The membership status cannot be changed") //TODO: InvalidMembershipStatusException
            }

        } else {
            throw Exception("User does not have permissions to do this operation") // TODO: PermissionDeniedException
        }
    }
}