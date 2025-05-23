package com.trailoapp.trailo_backend.dto.friendship.request

import com.trailoapp.trailo_backend.domain.enum.social.FriendshipStatus
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

data class UpdateFriendshipRequest (
    @field:NotNull(message = "Status is required")
    @field:Pattern(regexp = "ACCEPTED|PENDING|REJECTED", message = "Status must be ACCEPTED, PENDING or REJECTED")
    val status: String
)