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
import com.trailoapp.trailo_backend.domain.enum.geo.MeetupStatus
import com.trailoapp.trailo_backend.domain.enum.social.MembershipStatus
import com.trailoapp.trailo_backend.dto.common.response.ErrorResponse
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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page

@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Groups")
@SecurityRequirement(name = "Bearer Authentication")
class GroupController(
    private val groupService: GroupService,
    private val meetupService: MeetupService
) {

    // ===== BASIC GROUP OPERATIONS =====

    @PostMapping("/create")
    @Operation(
        summary = "Create a new group"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "201",
            description = "Group created successfully",
            content = [Content(schema = Schema(implementation = GroupResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid input data or validation errors",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409",
            description = "Group name already exists",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
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
    @Operation(
        summary = "Get a group details"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Group details retrieved successfully",
            content = [Content(schema = Schema(implementation = GroupResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Group not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun getGroup(
        @Parameter(description = "Group UUID") @PathVariable groupId: UUID
    ): ResponseEntity<GroupResponse> {
        val group = groupService.findGroupByUuid(groupId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(GroupResponse.fromEntity(group))
    }

    @PatchMapping("/{groupId}")
    @Operation(
        summary = "Update a group"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Group updated successfully",
            content = [Content(schema = Schema(implementation = GroupResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "User doesn't have permission to update this group",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Group not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun updateGroup(
        @Parameter(description = "Group UUID") @PathVariable groupId: UUID,
        @Valid @RequestBody request: UpdateGroupRequest,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<GroupResponse> {
        val updatedGroup = groupService.updateGroup(user.uuid, groupId, request)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(GroupResponse.fromEntity(updatedGroup))
    }

    @DeleteMapping("/{groupId}")
    @Operation(
        summary = "Delete a group"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "204",
            description = "Group deleted successfully"
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "User doesn't have permission to delete this group",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Group not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun deleteGroup(
        @Parameter(description = "Group UUID") @PathVariable groupId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        groupService.deleteGroup(groupId, user.uuid)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    // ===== SEARCH AND LISTING =====

    @GetMapping
    @Operation(
        summary = "Get all groups"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Groups retrieved successfully",
            content = [Content(schema = Schema(implementation = PageResponse::class))]
        )
    ])
    fun getAllGroups(
        @Parameter(description = "Page number") @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") size: Int,
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
    @Operation(
        summary = "Search groups",
        description = "Search groups by name or is_private"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Search results retrieved successfully",
            content = [Content(schema = Schema(implementation = PageResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid search parameters",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun searchGroups(
        @Parameter(description = "Query") @RequestParam(required = false, defaultValue = "") query: String,
        @Parameter(description = "Page number") @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") size: Int,
        @Parameter(description = "Search by name or is_private")
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
    @Operation(
        summary = "Get my groups"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "User groups retrieved successfully",
            content = [Content(schema = Schema(implementation = PageResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun getMyGroups(
        @Parameter(description = "Page number") @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") size: Int,
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
    @Operation(
        summary = "Get my favorite groups"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Favorite groups retrieved successfully",
            content = [Content(schema = Schema(implementation = PageResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun getFavoriteGroups(
        @Parameter(description = "Page number") @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") size: Int,
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

    @PostMapping("/{groupId}/join")
    @Operation(
        summary = "Join a group"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Group joined successfully or request sent",
            content = [Content(schema = Schema(implementation = UserGroupResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "User already member of group",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Group not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun joinGroup(
        @Parameter(description = "Group UUID") @PathVariable groupId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<UserGroupResponse> {
        val userGroup = groupService.joinGroup(user, groupId)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(UserGroupResponse.fromEntity(userGroup))
    }

    @DeleteMapping("/{groupId}/leave")
    @Operation(
        summary = "Leave a group"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "204",
            description = "Left group successfully"
        ),
        ApiResponse(
            responseCode = "400",
            description = "Leader cannot leave group or user not member",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Group not found or user not member",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun leaveGroup(
        @Parameter(description = "Group UUID") @PathVariable groupId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        groupService.leaveGroup(user.uuid, groupId)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @PostMapping("/{groupId}/favorite")
    @Operation(
        summary = "Toggle favorite group"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Favorite status toggled successfully"
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Group not found or user not member",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun favoriteGroup(
        @Parameter(description = "Group UUID") @PathVariable groupId: UUID,
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
    @Operation(
        summary = "Get group members"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Group members retrieved successfully",
            content = [Content(schema = Schema(implementation = GroupMemberResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Group not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun getMembers(
        @Parameter(description = "Group UUID") @PathVariable groupId: UUID,
        @Parameter(description = "Page number") @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") size: Int,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<GroupMemberResponse> {
        val pageable = PageRequest.of(page, size)

        val response = groupService.getGroupMembers(user.uuid, groupId, pageable)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(response)
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @Operation(
        summary = "Kick a member from a group"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "204",
            description = "Member kicked successfully"
        ),
        ApiResponse(
            responseCode = "400",
            description = "Cannot kick self or business rule violation",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "User doesn't have permission to kick members",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Group or member not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun kickMember(
        @Parameter(description = "Group UUID") @PathVariable groupId: UUID,
        @Parameter(description = "Member UUID") @PathVariable memberId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        groupService.kickMember(user.uuid, groupId, memberId)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @PatchMapping("/{groupId}/members/{memberId}/role")
    @Operation(
        summary = "Update a member role"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Member role updated successfully"
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid role or business rule violation",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "User doesn't have permission to update roles",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Group or member not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun updateMemberRole(
        @Parameter(description = "Group UUID") @PathVariable groupId: UUID,
        @Parameter(description = "Member UUID") @PathVariable memberId: UUID,
        @Valid @RequestBody request: UpdateRoleRequest,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        groupService.updateMemberRole(user.uuid, groupId, memberId, request.role)

        return ResponseEntity.status(HttpStatus.OK).build()
    }

    // ===== MEMBERSHIP REQUESTS =====

    @GetMapping("/{groupId}/requests")
    @Operation(
        summary = "Get pending membership requests"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Pending requests retrieved successfully",
            content = [Content(schema = Schema(implementation = PageResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "User doesn't have permission to view requests",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Group not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun getPendingRequests(
        @Parameter(description = "Group UUID") @PathVariable groupId: UUID,
        @Parameter(description = "Page number") @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") size: Int,
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
    @Operation(
        summary = "Accept membership request"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "204",
            description = "Request accepted successfully"
        ),
        ApiResponse(
            responseCode = "400",
            description = "Request cannot be accepted (not pending)",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "User doesn't have permission to accept requests",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Group or request not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun acceptMemberRequest(
        @Parameter(description = "Group UUID") @PathVariable groupId: UUID,
        @Parameter(description = "User UUID") @PathVariable userId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        groupService.updateMembershipRequest(user.uuid, groupId, userId, MembershipStatus.ACCEPTED)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @DeleteMapping("/{groupId}/requests/{userId}/reject")
    @Operation(
        summary = "Reject membership request"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Request rejected successfully"
        ),
        ApiResponse(
            responseCode = "400",
            description = "Request cannot be rejected (not pending)",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "User doesn't have permission to reject requests",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Group or request not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun rejectMemberRequest(
        @Parameter(description = "Group UUID") @PathVariable groupId: UUID,
        @Parameter(description = "User UUID") @PathVariable userId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        groupService.updateMembershipRequest(user.uuid, groupId, userId, MembershipStatus.REJECTED)

        return ResponseEntity.status(HttpStatus.OK).build()
    }

    // ===== GROUP MEETUPS =====

    @GetMapping("/{groupId}/meetups")
    @Operation(
        summary = "Get group meetups"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Group meetups retrieved successfully",
            content = [Content(schema = Schema(implementation = PageResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "User doesn't have permission to view private group meetups",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Group not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun getGroupMeetups(
        @Parameter(description = "Group UUID") @PathVariable groupId: UUID,
        @Parameter(description = "Page number") @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false, defaultValue = "WAITING") status: MeetupStatus,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<PageResponse<MeetupResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "meetingTime"))

        val meetups = meetupService.getGroupMeetupsByStatus(user.uuid, groupId, status, pageable)

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