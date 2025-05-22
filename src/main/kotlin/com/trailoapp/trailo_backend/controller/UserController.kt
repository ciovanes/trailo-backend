package com.trailoapp.trailo_backend.controller

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.dto.common.response.ErrorResponse
import com.trailoapp.trailo_backend.dto.common.response.PageResponse
import com.trailoapp.trailo_backend.dto.meetup.response.MeetupResponse
import com.trailoapp.trailo_backend.dto.user.request.UpdateUserRequest
import com.trailoapp.trailo_backend.dto.user.response.UserResponse
import com.trailoapp.trailo_backend.service.MeetupService
import com.trailoapp.trailo_backend.service.UserService
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
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
class UserController(
    private val userService: UserService,
    private val meetupService: MeetupService
) {

    // ===== USER MANAGEMENT =====

    @GetMapping("/me")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Get the current user"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "User retrieved successfully",
            content = [Content(schema = Schema(implementation = UserResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun getCurrentUser(@AuthenticationPrincipal user: UserEntity): ResponseEntity<UserResponse> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(UserResponse.fromUser(user))
    }

    @PatchMapping("/me")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Update the current user"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "User updated successfully",
            content = [Content(schema = Schema(implementation = UserResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid request body",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun updateUser(
        @Valid @RequestBody updateUserRequest: UpdateUserRequest,
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<UserResponse> {
        val updatedUser = userService.updateUser(user.uuid, updateUserRequest)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(UserResponse.fromUser(updatedUser))
    }

    @DeleteMapping("/me")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Delete the current user"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "204",
            description = "User deleted successfully"
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
        )
    ])
    fun deleteUser(
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        userService.deleteUser(user.uuid, user.username)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    // ===== SEARCH AND LISTING =====

    @GetMapping
    @Operation(
        summary = "Get all users"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = [Content(schema = Schema(implementation = PageResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid page number or page size",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun getAllUsers(
        @Parameter(description = "Page number") @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<UserResponse>> {
        val pageable = PageRequest.of(page, size)
        val userPage = userService.getAllUsers(pageable)

        val response = PageResponse(
            content = userPage.content.map { UserResponse.fromUser(it) },
            pageNumber = page,
            pageSize = size,
            totalElements = userPage.totalElements,
            totalPages = userPage.totalPages,
            isLast = userPage.isLast,
        )

        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search users"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = [Content(schema = Schema(implementation = PageResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid search parameters",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun searchUsers(
        @Parameter(description = "Search query") @RequestParam(required = false, defaultValue = "") query: String,
        @Parameter(description = "Page number") @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") size: Int,
        @Parameter(description = "Search field: username, name, surname, country")
            @RequestParam(required = false, defaultValue = "username") searchBy: String
    ): ResponseEntity<PageResponse<UserResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "username"))

        val userPage = userService.searchQuery(searchBy, query, pageable)

        val response = PageResponse(
            content = userPage.content.map { UserResponse.fromUser(it) },
            pageNumber = page,
            pageSize = size,
            totalElements = userPage.totalElements,
            totalPages = userPage.totalPages,
            isLast = userPage.isLast,
        )

        return ResponseEntity.status(HttpStatus.OK).body(response)
    }

    @GetMapping("/id/{uuid}")
    @Operation(
        summary = "Get a user by its ID"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "User retrieved successfully",
            content = [Content(schema = Schema(implementation = UserResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun getUserByEmail(
        @Parameter(description = "User UUID") @PathVariable uuid: UUID
    ): ResponseEntity<UserResponse> {
        val user = userService.findUserById(uuid)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(UserResponse.fromUser(user))
    }

    @GetMapping("/{username}")
    @Operation(
        summary = "Get a user by its username"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "User retrieved successfully",
            content = [Content(schema = Schema(implementation = UserResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun getUserByUsername(
        @Parameter(description = "Username") @PathVariable username: String
    ): ResponseEntity<UserResponse> {
        val user = userService.findUserByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(UserResponse.fromUser(user))
    }

    // ===== USER MEETUPS =====

    @GetMapping("/{userId}/meetups")
    @Operation(
        summary = "Get all meetups of a user"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Meetups retrieved successfully",
            content = [Content(schema = Schema(implementation = PageResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid page number or page size",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun getUserMeetups(
        @Parameter(description = "User UUID") @PathVariable userId: UUID,
        @Parameter(description = "Page number") @RequestParam(required = false, defaultValue = "0") page: Int,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<MeetupResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "meetingTime"))
        val meetups = meetupService.getUserMeetups(userId, pageable)

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