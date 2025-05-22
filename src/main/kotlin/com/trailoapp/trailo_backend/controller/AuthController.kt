package com.trailoapp.trailo_backend.controller

import com.trailoapp.trailo_backend.dto.auth.request.LoginUserRequest
import com.trailoapp.trailo_backend.dto.auth.request.RegisterUserRequest
import com.trailoapp.trailo_backend.dto.auth.response.AuthResponse
import com.trailoapp.trailo_backend.dto.user.response.UserResponse
import com.trailoapp.trailo_backend.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "201",
            description = "User registered successfully",
            content = [Content(schema = Schema(implementation = UserResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid request body",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409",
            description = "User already exists",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun register(@Valid @RequestBody request: RegisterUserRequest): ResponseEntity<UserResponse> {
        val user = authService.registerUser(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromUser(user))
    }

    @PostMapping("/login")
    @Operation(
        summary = "Login a user"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = [Content(schema = Schema(implementation = AuthResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid request body",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun login(@RequestBody request: LoginUserRequest): ResponseEntity<AuthResponse> {
        val authResponse = authService.loginUser(request)
        return ResponseEntity.status(HttpStatus.OK).body(authResponse)
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Logout a user",
        description = "Placeholder for future token invalidation"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "204",
            description = "Logout successful"
        ),
        ApiResponse(
            responseCode = "401",
            description = "User not authenticated",
        )
    ])
    fun logout(): ResponseEntity<Unit> {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}