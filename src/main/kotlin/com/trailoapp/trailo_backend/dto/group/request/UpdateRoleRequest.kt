package com.trailoapp.trailo_backend.dto.group.request

import com.trailoapp.trailo_backend.domain.enum.social.GroupRoles
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class UpdateRoleRequest(
    @field:NotBlank(message = "Role is required")
    @Schema(
        description = "Role of the group",
        allowableValues = ["LEADER", "CO_LEADER", "ELDER", "MEMBER"]
    )
    val role: GroupRoles
)
