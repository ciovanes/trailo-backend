package com.trailoapp.trailo_backend.service

import com.trailoapp.trailo_backend.domain.core.UserEntity
import com.trailoapp.trailo_backend.dto.auth.request.LoginUserRequest
import org.springframework.stereotype.Service
import jakarta.transaction.Transactional
import com.trailoapp.trailo_backend.dto.auth.request.RegisterUserRequest
import com.trailoapp.trailo_backend.dto.auth.response.AuthResponse
import com.trailoapp.trailo_backend.dto.user.request.CreateUserDto
import com.trailoapp.trailo_backend.dto.user.response.UserResponse
import java.time.OffsetDateTime


@Service
class AuthService(
    private val userService: UserService,
    private val cognitoService: CognitoService
) {

    /**
     * Registers a new user in both Cognito and local database.
     *
     * @param request Registration data for the new user.
     * @return The created user.
     */
    @Transactional
    fun registerUser(request: RegisterUserRequest): UserEntity {
        // Register with Cognito and get the ID
        val cognitoUserId = cognitoService.registerUser(
            email = request.email,
            password = request.password,
            username = request.username,
            name = request.name
        )

        // Create the local user
        return userService.createUser(
            CreateUserDto(
                email = request.email,
                username = request.username,
                name = request.name,
                surname = request.surname,
                profileImageUrl = request.profileImageUrl,
                country = request.country,
                cognitoId = cognitoUserId
            )
        )
    }

    /**
     * Authenticates a user using username/email and password.
     *
     * @param request Login request credentials.
     * @return Authentication response with tokens and user info.
     */
    fun loginUser(request: LoginUserRequest): AuthResponse {
        val authResult = cognitoService.loginUser(request.username, request.password)

        val user = findUserByIdentifier(request.username)

        // Update last login date
        user?.apply {
            lastLoginAt = OffsetDateTime.now()
            userService.saveUser(this)
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

    // ===== UTILITY METHODS ======

    /**
     * Finds a user by email or a username.
     *
     * @param identifier The user's identifier.
     * @return The found [UserEntity], or null.
     */
    private fun findUserByIdentifier(identifier: String): UserEntity? {
        return if (identifier.contains("@")) {
            userService.findUserByEmail(identifier)
        } else {
            userService.findUserByUsername(identifier)
        }
    }
}