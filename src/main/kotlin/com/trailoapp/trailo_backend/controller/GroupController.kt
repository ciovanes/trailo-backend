package com.trailoapp.trailo_backend.controller

import java.util.UUID
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.domain.enum.MeetupStatus
import com.trailoapp.trailo_backend.domain.enum.MembershipStatus
import com.trailoapp.trailo_backend.dto.common.response.PageResponse
import com.trailoapp.trailo_backend.dto.group.request.CreateGroupRequest
import com.trailoapp.trailo_backend.dto.group.request.UpdateGroupRequest
import com.trailoapp.trailo_backend.dto.group.request.UpdateRoleRequest
import com.trailoapp.trailo_backend.dto.group.response.GroupMemberResponse
import com.trailoapp.trailo_backend.dto.group.response.GroupResponse
import com.trailoapp.trailo_backend.dto.group.response.UserGroupResponse
import com.trailoapp.trailo_backend.dto.meetup.response.MeetupResponse
import com.trailoapp.trailo_backend.dto.user.response.UserResponse
import com.trailoapp.trailo_backend.service.GroupService
import com.trailoapp.trailo_backend.service.MeetupService
import org.springframework.data.domain.Page

@RestController
@RequestMapping("/api/v1/groups")
class GroupController(
    private val groupService: GroupService,
    private val meetupService: MeetupService
) {

    // ===== BASIC GROUP OPERATIONS =====

    @PostMapping("/create")
    fun createGroup(
        @Valid @RequestBody request: CreateGroupRequest,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<GroupResponse> {
        val group = groupService.createGroup(request, user)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(GroupResponse.fromEntity(group))
    }

    @GetMapping("/{groupId}")
    fun getGroup(
        @PathVariable groupId: UUID
    ): ResponseEntity<GroupResponse> {
        val group = groupService.findGroupByUuid(groupId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(GroupResponse.fromEntity(group))
    }

    @PatchMapping("/{groupId}")
    fun updateGroup(
        @PathVariable groupId: UUID,
        @Valid @RequestBody request: UpdateGroupRequest,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<GroupResponse> {
        val updatedGroup = groupService.updateGroup(user.uuid, groupId, request)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(GroupResponse.fromEntity(updatedGroup))
    }

    @DeleteMapping("/{groupId}")
    fun deleteGroup(
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        groupService.deleteGroup(groupId, user.uuid)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    // ===== SEARCH AND LISTING =====

    @GetMapping
    fun getAllGroups(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<GroupResponse>> {
        val pageable = PageRequest.of(page, size)
        val groupPage = groupService.getAllGroups(pageable)

        val response = PageResponse(
            content = groupPage.content.map { GroupResponse.fromEntity(it) },
            pageNumber = page,
            pageSize = size,
            totalElements = groupPage.totalElements,
            totalPages = groupPage.totalPages,
            isLast = groupPage.isLast,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(response)
    }


    @GetMapping("/search")
    fun searchGroups(
        @RequestParam(required = false, defaultValue = "") query: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false, defaultValue = "name") searchBy: String,
    ): ResponseEntity<PageResponse<GroupResponse>> {

        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))

        val groupPage = when (searchBy.lowercase()) {
            "name" -> groupService.searchByName(query, pageable)
            "is_private" -> groupService.searchByIsPrivate(query.toBooleanStrict(), pageable)
            else -> groupService.searchByName(query, pageable)
        }

        val response = PageResponse(
            content = groupPage.content.map { GroupResponse.fromEntity(it) },
            pageNumber =  page,
            pageSize = size,
            totalElements = groupPage.totalElements,
            totalPages = groupPage.totalPages,
            isLast = groupPage.isLast
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(response)
    }

    // ===== PERSONAL MEMBERSHIP OPERATIONS =====

    @GetMapping("/my")
    fun getMyGroups(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<PageResponse<GroupResponse>> {
        val pageable = PageRequest.of(page, size)

        val myGroups = groupService.getMyGroups(user.uuid, pageable)

        val response = PageResponse(
            content = myGroups.content.map { GroupResponse.fromEntity(it) },
            pageNumber = page,
            pageSize = size,
            totalElements = myGroups.totalElements,
            totalPages = myGroups.totalPages,
            isLast = myGroups.isLast
        )

        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    @GetMapping("/favorites")
    fun getFavoriteGroups(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<PageResponse<GroupResponse>> {
        val pageable = PageRequest.of(page, size)
        val favoritesGroups = groupService.getFavoriteGroups(user.uuid, pageable)

        val response = PageResponse(
            content = favoritesGroups.content.map { GroupResponse.fromEntity(it) },
            pageNumber = page,
            pageSize = size,
            totalElements = favoritesGroups.totalElements,
            totalPages = favoritesGroups.totalPages,
            isLast = favoritesGroups.isLast
        )

        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    @PostMapping("/join/{groupId}")
    fun joinGroup(
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<UserGroupResponse> {
        val userGroup = groupService.joinGroup(user, groupId)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(UserGroupResponse.fromEntity(userGroup))
    }

    @DeleteMapping("/{groupId}/leave")
    fun leaveGroup(
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        groupService.leaveGroup(user.uuid, groupId)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @PostMapping("/{groupId}/favorite")
    fun favoriteGroup(
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Map<String, Boolean>> {
        groupService.toggleFavorite(user.uuid, groupId)

        val isFavorite = groupService.checkIsFavorite(user.uuid, groupId)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(mapOf("isFavorite" to isFavorite))
    }

    // ===== MEMBERSHIP MANAGEMENT =====

    @GetMapping("/{groupId}/members")
    fun getMembers(
        @PathVariable groupId: UUID,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<GroupMemberResponse> {
        val pageable = PageRequest.of(page, size)

        val response = groupService.getGroupMembers(user.uuid, groupId, pageable)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(response)
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    fun kickMember(
        @PathVariable groupId: UUID,
        @PathVariable memberId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        groupService.kickMember(user.uuid, groupId, memberId)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @PatchMapping("/{groupId}/members/{memberId}/role")
    fun updateMemberRole(
        @PathVariable groupId: UUID,
        @PathVariable memberId: UUID,
        @Valid @RequestBody request: UpdateRoleRequest,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        groupService.updateMemberRole(user.uuid, groupId, memberId, request.role)

        return ResponseEntity.status(HttpStatus.OK).build()
    }

    // ===== MEMBERSHIP REQUESTS =====

    @GetMapping("/{groupId}/requests")
    fun getPendingRequests(
        @PathVariable groupId: UUID,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<PageResponse<UserResponse>> {
        val pageable = PageRequest.of(page, size)

        val pendingRequests = groupService.getPendingRequests(user.uuid, groupId, pageable)

        val response = PageResponse(
            content = pendingRequests.content.map { UserResponse.fromUser(it) },
            pageNumber = page,
            pageSize = size,
            totalElements = pendingRequests.totalElements,
            totalPages = pendingRequests.totalPages,
            isLast = pendingRequests.isLast
        )

        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    @PatchMapping("/{groupId}/requests/{userId}/accept")
    fun acceptMemberRequest(
        @PathVariable groupId: UUID,
        @PathVariable userId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        groupService.updateMembershipRequest(user.uuid, groupId, userId, MembershipStatus.ACCEPTED)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @DeleteMapping("/{groupId}/requests/{userId}/reject")
    fun rejectMemberRequest(
        @PathVariable groupId: UUID,
        @PathVariable userId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        groupService.updateMembershipRequest(user.uuid, groupId, userId, MembershipStatus.REJECTED)

        return ResponseEntity.status(HttpStatus.OK).build()
    }


    // ===== MEETUPS =====
    @GetMapping("/{groupId}/meetups")
    fun getGroupMeetups(
        @PathVariable groupId: UUID,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false, defaultValue = "WAITING") status: MeetupStatus,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<PageResponse<MeetupResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "meetingTime"))

        val meetups = meetupService.findGroupMeetups(user.uuid, groupId, status, pageable)

        val response = PageResponse(
            content = meetups.content.map { MeetupResponse.fromEntity(it) },
            pageNumber = page,
            pageSize = size,
            totalElements = meetups.totalElements,
            totalPages = meetups.totalPages,
            isLast = meetups.isLast
        )

        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

}