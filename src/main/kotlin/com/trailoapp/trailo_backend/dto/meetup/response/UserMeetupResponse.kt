package com.trailoapp.trailo_backend.dto.meetup.response

import com.trailoapp.trailo_backend.domain.social.UserMeetupEntity
import com.trailoapp.trailo_backend.dto.user.response.UserResponse
import java.time.OffsetDateTime
import java.util.UUID

data class UserMeetupResponse (
    val uuid: UUID,
    val user: UserResponse,
    val meetup: MeetupResponse,
    val joinDate: OffsetDateTime
) {
    companion object {
        fun fromEntity(userMeetupEntity: UserMeetupEntity): UserMeetupResponse {
            return UserMeetupResponse(
                uuid = userMeetupEntity.uuid,
                user = UserResponse.fromUser(userMeetupEntity.user),
                meetup = MeetupResponse.fromEntity(userMeetupEntity.meetup),
                joinDate = userMeetupEntity.joinDate
            )
        }
    }
}