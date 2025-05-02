package com.trailoapp.trailo_backend.dto.friendship.response

import com.trailoapp.trailo_backend.domain.social.FriendshipEntity
import com.trailoapp.trailo_backend.dto.user.response.UserResponse
import java.time.OffsetDateTime
import java.util.UUID

data class FriendshipResponse (
    val uuid: UUID,
    val friend: UserResponse,
    val status: String,
    val createdAt: OffsetDateTime,
    val lastModifiedAt: OffsetDateTime?
) {
    companion object {
        fun fromEntity(friendship: FriendshipEntity, viewerId: UUID): FriendshipResponse {
            val otherUser = if (friendship.user.uuid == viewerId) {
                friendship.friend
            } else {
                friendship.user
            }

            return FriendshipResponse(
                uuid = friendship.uuid,
                friend = UserResponse.fromUser(otherUser),
                status = friendship.status.name,
                createdAt = friendship.createdAt,
                lastModifiedAt = friendship.lastModifiedAt,
            )
        }
    }
}