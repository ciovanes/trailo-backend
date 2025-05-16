package com.trailoapp.trailo_backend.controller

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.domain.enum.FriendshipStatus
import com.trailoapp.trailo_backend.dto.common.response.PageResponse
import com.trailoapp.trailo_backend.service.FriendshipService
import org.springframework.http.HttpStatus
import com.trailoapp.trailo_backend.dto.friendship.request.SendFriendRequestRequest
import com.trailoapp.trailo_backend.dto.friendship.request.UpdateFriendshipRequest
import com.trailoapp.trailo_backend.dto.friendship.response.FriendshipResponse
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/friendships")
class FriendshipController(
    private val friendshipService: FriendshipService
) {

    // ===== BASIC FRIENDSHIP OPERATIONS =====

    @GetMapping("/friends")
    fun getFriends(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
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
    fun updateFriendshipStatus(
        @PathVariable uuid: UUID,
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
    fun deleteFriend(
        @PathVariable friendId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Void> {
        friendshipService.deleteFriendship(user.uuid, friendId)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    // ===== LISTING AND CHECK =====

    @GetMapping("/sent")
    fun getPendingRequests(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
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
    fun checkFriendship(
        @PathVariable userId: UUID,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<FriendshipResponse> {
        val friendship = friendshipService.findByUsersIds(user.uuid, userId)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(FriendshipResponse.fromEntity(friendship, user.uuid))
    }
}