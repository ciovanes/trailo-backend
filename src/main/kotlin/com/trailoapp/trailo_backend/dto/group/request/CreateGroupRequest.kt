package com.trailoapp.trailo_backend.dto.group.request

import jakarta.validation.constraints.NotNull

data class CreateGroupRequest(
    @field:NotNull(message = "Group name is required")
    val name: String,

    val description: String? = null,

    @field:NotNull(message = "Group privacy is required")
    val isPrivate: Boolean,

    val imageUrl: String? = null,
)
