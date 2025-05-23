package com.trailoapp.trailo_backend.controller

import com.trailoapp.trailo_backend.dto.auth.request.LoginUserRequest
import com.trailoapp.trailo_backend.dto.auth.request.RegisterUserRequest
import com.trailoapp.trailo_backend.dto.auth.response.AuthResponse
import com.trailoapp.trailo_backend.dto.user.response.UserResponse
import com.trailoapp.trailo_backend.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterUserRequest): ResponseEntity<UserResponse> {
        val user = authService.registerUser(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromUser(user))
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginUserRequest): ResponseEntity<AuthResponse> {
        val authResponse = authService.loginUser(request)
        return ResponseEntity.status(HttpStatus.OK).body(authResponse)
    }

    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Unit> {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}