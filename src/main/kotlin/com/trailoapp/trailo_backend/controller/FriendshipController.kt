package com.trailoapp.trailo_backend.controller

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.domain.enum.social.FriendshipStatus
import com.trailoapp.trailo_backend.dto.common.response.ErrorResponse
import com.trailoapp.trailo_backend.dto.common.response.PageResponse
import com.trailoapp.trailo_backend.service.FriendshipService
import com.trailoapp.trailo_backend.dto.friendship.request.SendFriendRequestRequest
import com.trailoapp.trailo_backend.dto.friendship.request.UpdateFriendshipRequest
import com.trailoapp.trailo_backend.dto.friendship.response.FriendshipResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/friendships")
@Tag(name = "Friendships")
@SecurityRequirement(name = "Bearer Authentication")
class FriendshipController(
    private val friendshipService: FriendshipService
) {

    // ===== BASIC FRIENDSHIP OPERATIONS =====

    @GetMapping("/friends")
    @Operation(
        summary = "Get user's friends"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Friends retrieved successfully",
            content = [Content(schema = Schema(implementation = PageResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun getFriends(
        @Parameter(description = "Page number") @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") size: Int,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<PageResponse<FriendshipResponse>> {
        val pageable = PageRequest.of(page, size)
        val friendships = friendshipService.getFriends(user.uuid, pageable)

        val response = PageResponse(
            content = friendships.content.map { FriendshipResponse.fromEntity(it, user.uuid) },
            pageNumber = page,
            pageSize = pageable.pageSize,
            totalElements = friendships.totalElements,
            totalPages = friendships.totalPages,
            isLast = friendships.isLast
        )

        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    @PostMapping("/add")
    @Operation(
        summary = "Send friend request",
        description = "Sends a friend request to another user."
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Friend request sent or friendship auto-accepted",
            content = [Content(schema = Schema(implementation = FriendshipResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid request or business rule violation",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Target user not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun sendFriendRequest(
        @Valid @RequestBody request: SendFriendRequestRequest,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<FriendshipResponse> {
        val friendship = friendshipService.sendFriendRequest(user.uuid, request.friendId)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(FriendshipResponse.fromEntity(friendship, user.uuid))
    }

    @PatchMapping("/{uuid}")
    @Operation(
        summary = "Update friendship status",
        description = "Updates the status of a friendship (ACCEPTED/REJECTED)."
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Friendship status updated successfully",
            content = [Content(schema = Schema(implementation = FriendshipResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid status or business rule violation",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "403",
            description = "User doesn't have permission to update this friendship",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Friendship not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun updateFriendshipStatus(
        @Parameter(description = "Friendship UUID") @PathVariable uuid: UUID,
        @Valid @RequestBody request: UpdateFriendshipRequest,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<FriendshipResponse> {
        val status = FriendshipStatus.valueOf(request.status)
        val friendship = friendshipService.updateFriendshipStatus(uuid, user.uuid, status)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(FriendshipResponse.fromEntity(friendship, user.uuid))
    }

    @DeleteMapping("/{friendId}")
    @Operation(
        summary = "Delete friendship",
        description = "Removes a friendship between the sender user and another user"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "204",
            description = "Friendship deleted successfully"
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Friendship not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun deleteFriend(
        @Parameter(description = "Friend's user UUID") @PathVariable friendId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Void> {
        friendshipService.deleteFriendship(user.uuid, friendId)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    // ===== LISTING AND CHECK =====

    @GetMapping("/sent")
    @Operation(
        summary = "Get pending sent friend requests"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Pending requests retrieved successfully",
            content = [Content(schema = Schema(implementation = PageResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun getPendingRequests(
        @Parameter(description = "Page number") @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") size: Int,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<PageResponse<FriendshipResponse>> {
        val pageable = PageRequest.of(page, size)
        val pendingRequests = friendshipService.getPendingRequests(user.uuid, pageable)

        val response = PageResponse(
            content = pendingRequests.content.map { FriendshipResponse.fromEntity(it, user.uuid) },
            pageNumber = page,
            pageSize = size,
            totalElements = pendingRequests.totalElements,
            totalPages = pendingRequests.totalPages,
            isLast = pendingRequests.isLast
        )

        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    @GetMapping("/check/{userId}")
    @Operation(
        summary = "Check friendship status"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Friendship status retrieved successfully",
            content = [Content(schema = Schema(implementation = FriendshipResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "No friendship exists between users",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun checkFriendship(
        @Parameter(description = "User UUID") @PathVariable userId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<FriendshipResponse> {
        val friendship = friendshipService.findByUsersIds(user.uuid, userId)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(FriendshipResponse.fromEntity(friendship, user.uuid))
    }
}