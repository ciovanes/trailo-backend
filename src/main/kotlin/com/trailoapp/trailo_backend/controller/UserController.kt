package com.trailoapp.trailo_backend.controller

import com.trailoapp.trailo_backend.dto.request.CreateUserRequest
import com.trailoapp.trailo_backend.dto.response.UserResponse
import com.trailoapp.trailo_backend.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {

    @PostMapping
    fun createUser(@RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        val user = userService.createUser(request.email, request.username)
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromUser(user))
    }

    @GetMapping
    fun getAllUsers(): ResponseEntity<List<UserResponse>> {
        val users = userService.getAllUsers()

        val userResponseList = users.map { UserResponse.fromUser(it) }
        return ResponseEntity.ok(userResponseList)
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