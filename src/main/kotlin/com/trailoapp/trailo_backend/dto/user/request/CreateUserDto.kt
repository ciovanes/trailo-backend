package com.trailoapp.trailo_backend.dto.user.request

data class CreateUserDto(
    val email: String,
    val username: String,
    val name: String? = null,
    val surname: String? = null,
    val profileImageUrl: String? = null,
    val country: String? = null,
    val cognitoId: String
)