package com.trailoapp.trailo_backend.dto.group.response

data class PrivateGroupMembersResponse (
    val totalElements: Long,
    override val isPrivate: Boolean = true
): GroupMemberResponse