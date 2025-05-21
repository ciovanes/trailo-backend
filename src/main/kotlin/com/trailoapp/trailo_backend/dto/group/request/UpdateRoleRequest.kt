package com.trailoapp.trailo_backend.dto.group.request

import com.trailoapp.trailo_backend.domain.enum.social.GroupRoles
import jakarta.validation.constraints.NotBlank

data class UpdateRoleRequest(
    @field:NotBlank(message = "Role is required")
    val role: GroupRoles
)
