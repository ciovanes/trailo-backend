package com.trailoapp.trailo_backend.utils

import com.trailoapp.trailo_backend.dto.common.response.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

fun buildErrorResponse(
    status: HttpStatus,
    error: String,
    message: String?
): ResponseEntity<ErrorResponse> {
    return ResponseEntity
        .status(status)
        .body(
            ErrorResponse(
                status = status.value(),
                error = error,
                message = message ?: "An error occurred"
            )
        )
}
