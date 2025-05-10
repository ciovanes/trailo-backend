package com.trailoapp.trailo_backend.dto.group.request

import com.trailoapp.trailo_backend.domain.enum.MembershipStatus

data class UpdateStatusRequest(
    val status: MembershipStatus
)
