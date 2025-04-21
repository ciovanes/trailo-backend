package com.trailoapp.trailo_backend.dto.response

import com.trailoapp.trailo_backend.domain.core.UserEntity
import java.util.*

data class UserResponse(
    val uuid: UUID,
    val email: String,
    val username: String
    ) {
    companion object {
        fun fromUser(userEntity: UserEntity): UserResponse {
            return UserResponse(
                uuid = userEntity.uuid,
                email = userEntity.email,
                username = userEntity.username
            )
        }
    }
}