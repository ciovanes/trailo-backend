package com.trailoapp.trailo_backend.controller

import com.trailoapp.trailo_backend.dto.common.response.PageResponse
import com.trailoapp.trailo_backend.dto.user.request.UpdateUserRequest
import com.trailoapp.trailo_backend.dto.user.response.UserResponse
import com.trailoapp.trailo_backend.service.UserService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {

    /*
    GET all users with pagination
     */
    @GetMapping
    fun getAllUsers(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ResponseEntity<PageResponse<UserResponse>> {

        val pageable = PageRequest.of(page, size)
        val userPage = userService.getAllUsers(pageable)

        val userResponseList = userPage.content.map { UserResponse.fromUser(it) }

        val response = PageResponse(
            content = userResponseList,
            pageNumber = page,
            pageSize = size,
            totalElements = userPage.totalElements,
            totalPages = userPage.totalPages,
            isLast = userPage.isLast,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(response)
    }

    /*
    Search users by different fields
     */
    @GetMapping("/search")
    fun searchUsers(
        @RequestParam(required = false, defaultValue = "") query: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false, defaultValue = "username") searchBy: String,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<PageResponse<UserResponse>> {

        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "username"))

        val userPage = when (searchBy.lowercase()) {
            "username" -> userService.searchByUsername(query, pageable)
            "name" -> userService.searchByName(query, pageable)
            "surname" -> userService.searchBySurname(query, pageable)
            "country" -> userService.searchByCountry(query, pageable)
            else -> userService.searchByUsername(query, pageable)
        }

        val response = PageResponse(
            content = userPage.content.map { UserResponse.fromUser(it) },
            pageNumber = page,
            pageSize = size,
            totalElements = userPage.totalElements,
            totalPages = userPage.totalPages,
            isLast = userPage.isLast,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(response)
    }

    /*
    GET user by UUID
     */
    @GetMapping("/id/{uuid}")
    fun getUserByEmail(@PathVariable uuid: UUID, @AuthenticationPrincipal jwt: Jwt): ResponseEntity<UserResponse> {
        val user = userService.findUserById(uuid)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(UserResponse.fromUser(user))
    }

    /*
    GET user by username
     */
    @GetMapping("/{username}")
    fun getUserByUsername(@PathVariable username: String): ResponseEntity<UserResponse> {
        val user = userService.findUserByUsername(username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(UserResponse.fromUser(user))
    }


    /*
    GET current user info
     */
    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<UserResponse> {
        val cognitoUserId = jwt.claims["sub"] as String

        val user = userService.findUserByCognitoId(cognitoUserId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(UserResponse.fromUser(user))
    }

    /*
    Update current user data
     */
    @PatchMapping("/me")
    fun updateUser(
        @Valid @RequestBody updateUserRequest: UpdateUserRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<UserResponse> {
        val cognitoUserId = jwt.claims["sub"] as String

        val user = userService.findUserByCognitoId(cognitoUserId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val updatedUser = userService.updateUser(user.uuid, updateUserRequest)

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(UserResponse.fromUser(updatedUser))
    }

    /*
    Delete current user
     */
    @DeleteMapping("/sayonara_baby")
    fun deleteUser(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<Unit> {
        val cognitoUserId = jwt.claims["sub"] as String

        val user = userService.findUserByCognitoId(cognitoUserId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        userService.deleteUser(user.uuid, user.username)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}