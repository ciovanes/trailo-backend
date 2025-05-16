package com.trailoapp.trailo_backend.controller

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.dto.common.response.PageResponse
import com.trailoapp.trailo_backend.dto.user.request.UpdateUserRequest
import com.trailoapp.trailo_backend.dto.user.response.UserResponse
import com.trailoapp.trailo_backend.exception.definitions.ResourceNotFoundException
import com.trailoapp.trailo_backend.service.UserService
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
class UserController(
    private val userService: UserService
) {

    // ===== USER MANAGEMENT =====

    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal user: UserEntity): ResponseEntity<UserResponse> {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(UserResponse.fromUser(user))
    }

    @PatchMapping("/me")
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
    fun deleteUser(
        @AuthenticationPrincipal user: UserEntity
    ): ResponseEntity<Unit> {
        userService.deleteUser(user.uuid, user.username)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    // ===== SEARCH AND LISTING =====

    @GetMapping
    fun getAllUsers(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
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
    fun searchUsers(
        @RequestParam(required = false, defaultValue = "") query: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
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
    fun getUserByEmail(
        @PathVariable uuid: UUID
    ): ResponseEntity<UserResponse> {
        val user = userService.findUserById(uuid)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(UserResponse.fromUser(user))
    }

    @GetMapping("/{username}")
    fun getUserByUsername(
        @PathVariable username: String
    ): ResponseEntity<UserResponse> {
        val user = userService.findUserByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(UserResponse.fromUser(user))
    }
}