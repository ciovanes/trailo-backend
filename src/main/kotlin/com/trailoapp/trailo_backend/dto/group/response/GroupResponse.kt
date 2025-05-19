package com.trailoapp.trailo_backend.dto.group.response

import com.trailoapp.trailo_backend.domain.core.GroupEntity
import java.util.UUID

data class GroupResponse(
    val uuid: UUID,
    val name: String,
    val description: String? = null,
    val isPrivate: Boolean,
    val imageUrl: String? = null,
    val lastModifiedAt: String? = null,
    val createdAt: String?,
) {
    companion object {
        fun fromEntity(group: GroupEntity): GroupResponse {
            return GroupResponse(
                uuid = group.uuid,
                name = group.name,
                description = group.description,
                isPrivate = group.isPrivate,
                imageUrl = group.imageUrl,
                lastModifiedAt = group.lastModifiedAt?.toString(),
                createdAt = group.createdAt.toString()
            )
        }
    }
}
