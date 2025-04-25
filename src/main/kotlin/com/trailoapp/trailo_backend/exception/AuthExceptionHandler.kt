package com.trailoapp.trailo_backend.exception

import com.trailoapp.trailo_backend.dto.response.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException

/*
Handle exceptions that occur during authentication
 */
@RestControllerAdvice
class AuthExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(NotAuthorizedException::class)
    fun handleNotAuthorizedException(ex: NotAuthorizedException): ResponseEntity<ErrorResponse> {
        logger.warn("Not authorized: ${ex.message}")

        return ResponseEntity.status(
            HttpStatus.UNAUTHORIZED
        ).body(
            ErrorResponse(
                status = HttpStatus.UNAUTHORIZED.value(),
                error = "Not authorized",
                message = ex.message.toString()
            ))
    }

    @ExceptionHandler(UsernameExistsException::class)
    fun handleUsernameExistsException(ex: UsernameExistsException): ResponseEntity<ErrorResponse> {
        logger.warn("Username already exists: ${ex.message}")

        return ResponseEntity.status(
            HttpStatus.BAD_REQUEST
        ).body(
            ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Username already exists"
            )
        )
    }

}