package com.trailoapp.trailo_backend.dto.group.request

import java.util.UUID

data class InviteUsersRequest (
    val userIds: List<UUID>
)
