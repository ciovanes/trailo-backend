package com.trailoapp.trailo_backend.dto.friendship.request

import org.jetbrains.annotations.NotNull
import java.util.UUID

data class SendFriendRequestRequest (
    @field:NotNull("Friend ID is required")
    val friendId: UUID
)