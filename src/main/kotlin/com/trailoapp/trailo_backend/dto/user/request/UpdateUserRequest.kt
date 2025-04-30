package com.trailoapp.trailo_backend.dto.user.request

data class UpdateUserRequest(
    val name: String?,
    val surname: String?,
    val profileImageUrl: String?,
    val country: String?
)
