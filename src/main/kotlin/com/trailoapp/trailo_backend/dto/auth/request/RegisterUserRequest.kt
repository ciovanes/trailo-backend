package com.trailoapp.trailo_backend.dto.auth.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterUserRequest (
    @field:NotBlank(message = "Email must not be blank")
    @field:Email(message = "Email must be a valid email address")
    val email: String,

    @field:NotBlank(message = "Username must not be blank")
    @field:Size(min = 3, max = 24, message = "Username must be between 3 and 24 characters")
    val username: String,

    @field:NotBlank(message = "Password must not be blank")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    @field:Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
        message = "Password must contain at least one digit, lowercase, uppercase and special character"
    )
    val password: String,

    val name: String? = null,
    val surname: String? = null,
    val profileImageUrl: String? = null,
    val country: String? = null
)