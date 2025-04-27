package com.trailoapp.trailo_backend.dto.common.response

import java.time.LocalDateTime

data class ErrorResponse(
    val timestamp: String = LocalDateTime.now().toString(),
    val status: Int,
    val error: String,
    val message: String? = ""
)