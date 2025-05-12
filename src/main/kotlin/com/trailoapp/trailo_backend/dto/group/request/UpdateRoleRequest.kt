package com.trailoapp.trailo_backend.dto.group.request

import com.trailoapp.trailo_backend.domain.enum.GroupRoles

data class UpdateRoleRequest(
    val role: GroupRoles
)
