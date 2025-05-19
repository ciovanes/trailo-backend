package com.trailoapp.trailo_backend.dto.user.response

import com.trailoapp.trailo_backend.domain.core.UserEntity
import java.util.UUID

data class UserResponse(
    val uuid: UUID,
    val email: String,
    val username: String,
    val cognitoId: String,
    val name: String? = null,
    val surname: String? = null,
    val profileImageUrl: String? = null,
    val country: String? = null,
    val lastLoginAt: String? = null,
    val lastModifiedAt: String? = null
    ) {
    companion object {
        fun fromUser(userEntity: UserEntity): UserResponse {
            return UserResponse(
                uuid = userEntity.uuid,
                email = userEntity.email,
                username = userEntity.username,
                cognitoId = userEntity.cognitoId,
                name = userEntity.name,
                surname = userEntity.surname,
                profileImageUrl = userEntity.profileImageUrl,
                country = userEntity.country,
                lastLoginAt = userEntity.lastLoginAt?.toString(),
                lastModifiedAt = userEntity.lastModifiedAt.toString()
            )
        }
    }
}