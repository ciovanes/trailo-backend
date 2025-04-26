package com.trailoapp.trailo_backend.controller

import com.trailoapp.trailo_backend.dto.request.CreateUserRequest
import com.trailoapp.trailo_backend.dto.response.PageResponse
import com.trailoapp.trailo_backend.dto.response.UserResponse
import com.trailoapp.trailo_backend.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {

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

    @GetMapping("/id/{uuid}")
    fun getUserByEmail(@PathVariable uuid: UUID): ResponseEntity<UserResponse> {
        val user = userService.findUserById(uuid)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(UserResponse.fromUser(user))
    }

    @GetMapping("/email/{email}")
    fun getUserByEmail(@PathVariable email: String): ResponseEntity<UserResponse> {
        val user = userService.findUserByEmail(email)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(UserResponse.fromUser(user))
    }

    @GetMapping("/username/{username}")
    fun getUserByUsername(@PathVariable username: String): ResponseEntity<UserResponse> {
        val user = userService.findUserByUsername(username)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(UserResponse.fromUser(user))
    }
}