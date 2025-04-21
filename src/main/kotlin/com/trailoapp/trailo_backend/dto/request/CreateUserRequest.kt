package com.trailoapp.trailo_backend.dto.request

data class CreateUserRequest(
    val email: String,
    val username: String
)