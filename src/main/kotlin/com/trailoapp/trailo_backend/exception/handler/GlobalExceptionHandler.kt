package com.trailoapp.trailo_backend.exception.handler

import com.trailoapp.trailo_backend.dto.common.response.ErrorResponse
import com.trailoapp.trailo_backend.exception.definitions.BusinessRuleException
import com.trailoapp.trailo_backend.exception.definitions.DuplicateResourceException
import com.trailoapp.trailo_backend.exception.definitions.PermissionDeniedException
import com.trailoapp.trailo_backend.exception.definitions.ResourceNotFoundException
import com.trailoapp.trailo_backend.exception.definitions.SelfActionException
import com.trailoapp.trailo_backend.utils.buildErrorResponse

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // ===== VALIDATION ERRORS =====

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException) =
        buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation error",
            ex.bindingResult.fieldErrors.joinToString(", ") { it.defaultMessage.toString() }
        )

    // Handle empty request body or missing required fields
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid request body: ${ex.message}")

        val message = ex.message?.let {
            val fieldPattern = "parameter (\\w+) which is".toRegex()
            val matchResult = fieldPattern.find(it)?.groupValues?.getOrNull(1)
            "Required field '${matchResult ?: "unknown"}' is missing or null"
        } ?: "Invalid request body"

        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Invalid request body",
            message
        )
    }

    // Handle invalid page or size
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(ex: MethodArgumentTypeMismatchException) =
        buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Invalid page or size",
            "Page or size must be a positive integer"
        )

    // ===== BUSINESS RULE EXCEPTIONS =====

    @ExceptionHandler(BusinessRuleException::class)
    fun handleBusinessRuleException(ex: BusinessRuleException) =
        buildErrorResponse(HttpStatus.BAD_REQUEST, "Business rule violation", ex.message)

    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicateResourceException(ex: DuplicateResourceException) =
        buildErrorResponse(HttpStatus.CONFLICT, "Duplicate resource", ex.message)

    @ExceptionHandler(SelfActionException::class)
    fun handleSelfActionException(ex: SelfActionException) =
        buildErrorResponse(HttpStatus.BAD_REQUEST, "Self action", ex.message)

    // ===== SECURITY EXCEPTIONS =====

    @ExceptionHandler(PermissionDeniedException::class)
    fun handlePermissionDeniedException(ex: PermissionDeniedException) =
        buildErrorResponse(HttpStatus.FORBIDDEN, "Permission denied", ex.message)

    @ExceptionHandler(NotAuthorizedException::class)
    fun handleNotAuthorizedException(ex: NotAuthorizedException) =
        buildErrorResponse(HttpStatus.UNAUTHORIZED, "Not authorized", ex.message)

    // ===== RESOURCE EXCEPTIONS =====

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(ex: ResourceNotFoundException) =
        buildErrorResponse(HttpStatus.NOT_FOUND, "Resource not found", ex.message)

    @ExceptionHandler(UsernameExistsException::class)
    fun handleUsernameExistsException(ex: UsernameExistsException) =
        buildErrorResponse(HttpStatus.BAD_REQUEST, "Username already exists", ex.message)

    // ===== FALLBACK HANDLER =====

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception: $ex")
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
            "An unexpected error occurred"
        )
    }
}