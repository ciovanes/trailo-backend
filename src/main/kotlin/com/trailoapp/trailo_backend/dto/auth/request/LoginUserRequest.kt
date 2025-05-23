package com.trailoapp.trailo_backend.dto.auth.request

import jakarta.validation.constraints.NotBlank

data class LoginUserRequest (
    @field:NotBlank(message = "Username is required")
    val username: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)