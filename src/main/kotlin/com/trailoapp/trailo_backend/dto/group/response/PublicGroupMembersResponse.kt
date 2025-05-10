package com.trailoapp.trailo_backend.dto.group.response

import com.trailoapp.trailo_backend.dto.user.response.UserResponse

data class PublicGroupMembersResponse (
    val members: List<UserResponse>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean,
    override val isPrivate: Boolean = false,
): GroupMemberResponse