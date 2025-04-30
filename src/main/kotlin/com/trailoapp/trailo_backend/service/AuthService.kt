package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.dto.auth.request.LoginUserRequest
import org.springframework.stereotype.Service
import jakarta.transaction.Transactional
import com.trailoapp.trailo_backend.dto.auth.request.RegisterUserRequest
import com.trailoapp.trailo_backend.dto.auth.response.AuthResponse
import com.trailoapp.trailo_backend.dto.user.response.UserResponse
import java.time.OffsetDateTime


@Service
class AuthService(
    private val userService: UserService,
    private val cognitoService: CognitoService
) {

    @Transactional
    fun registerUser(request: RegisterUserRequest): UserEntity {
        // Register a user with Cognito
        val cognitoUserId = cognitoService.registerUser(
            email = request.email,
            password = request.password,
            username = request.username,
            name = request.name
        )

        return userService.createUser(
            email = request.email,
            username = request.username,
            name = request.name,
            surname = request.surname,
            profileImageUrl = request.profileImageUrl,
            country = request.country,
            cognitoId = cognitoUserId
        )
    }

    fun loginUser(request: LoginUserRequest): AuthResponse {
        val authResult = cognitoService.loginUser(request.username, request.password)

        val user = if (request.username.contains("@")) {
            userService.findUserByEmail(request.username)
        } else {
            userService.findUserByUsername(request.username)
        }

        // Update last login date
        user?.let {
            it.lastLoginAt = OffsetDateTime.now()
            userService.saveUser(it)
        }

        return AuthResponse(
            accessToken = authResult.accessToken(),
            idToken = authResult.idToken(),
            refreshToken = authResult.refreshToken(),
            expiresIn = authResult.expiresIn(),
            tokenType = authResult.tokenType(),
            user = user?.let { UserResponse.fromUser(it) }
        )
    }
}