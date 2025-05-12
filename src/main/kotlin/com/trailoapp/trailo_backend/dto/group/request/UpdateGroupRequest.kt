package com.trailoapp.trailo_backend.dto.group.request

data class UpdateGroupRequest(
    val description: String? = null,
    val isPrivate: Boolean? = null,
    val imageUrl: String? = null
)
