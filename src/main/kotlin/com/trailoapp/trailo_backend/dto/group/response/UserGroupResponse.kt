package com.trailoapp.trailo_backend.dto.group.response

import com.trailoapp.trailo_backend.domain.enum.GroupRoles
import com.trailoapp.trailo_backend.domain.enum.MembershipStatus
import com.trailoapp.trailo_backend.domain.social.UserGroupEntity
import com.trailoapp.trailo_backend.dto.user.response.UserResponse
import java.util.UUID

data class UserGroupResponse(
    val uuid: UUID,
    val group: GroupResponse,
    val user: UserResponse,
    val status: MembershipStatus,
    val role: GroupRoles,
    val invitedBy: UUID,
    val isFavorite: Boolean,
    val lastModifiedAt: String? = null,
    val joinDate: String
) {
    companion object {
        fun fromEntity(userGroup: UserGroupEntity): UserGroupResponse {
            return UserGroupResponse(
                uuid = userGroup.uuid,
                group = GroupResponse.fromEntity(userGroup.group),
                user = UserResponse.fromUser(userGroup.user),
                status = userGroup.status,
                role = userGroup.role,
                invitedBy = userGroup.invitedBy,
                isFavorite = userGroup.isFavorite,
                lastModifiedAt = userGroup.lastModifiedAt?.toString(),
                joinDate = userGroup.joinDate.toString()
            )
        }
    }
}
