package com.trailoapp.trailo_backend.exception

import com.trailoapp.trailo_backend.dto.response.ErrorResponse

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception: $ex")

        return ResponseEntity.status(
            HttpStatus.INTERNAL_SERVER_ERROR
        ).body(
            ErrorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                error = "Internal server error",
                message = "An unexpected error occurred"
            ))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        logger.warn("Validation error: ${ex.message}")

        return ResponseEntity.status(
            HttpStatus.BAD_REQUEST
        ).body(
            ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Validation error",
                message = ex.bindingResult.fieldErrors.joinToString(", ") { it.defaultMessage.toString() }
            ))
    }

    /*
    Handle empty request body or missing required fields
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid request body: ${ex.message}")

        val message = ex.message?.let {
            val fieldPattern = "parameter (\\w+) which is".toRegex()
            val matchResult = fieldPattern.find(it)?.groupValues?.getOrNull(1)
            "Required field '${matchResult ?: "unknown"}' is missing or null'"
        } ?: "Invalid request body"

        return ResponseEntity.status(
            HttpStatus.BAD_REQUEST
        ).body(
            ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Invalid request body",
                message = message
            ))
    }

}