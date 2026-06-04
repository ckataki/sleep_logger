package com.noom.interview.fullstack.sleep

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler


@ControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        log.warn("Malformed JSON request body", ex)
        return ResponseEntity(
            ErrorResponse("bad_request", "Malformed JSON request body: ${ex.message}"),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        log.warn("Validation failed", ex)
        val messages = ex.bindingResult.fieldErrors.joinToString("; ") {
            "${it.field}: ${it.defaultMessage}"
        }
        return ResponseEntity(
            ErrorResponse("validation_failed", messages),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        log.warn("Invalid argument", ex)
        return ResponseEntity(
            ErrorResponse("bad_request", ex.message),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(ex: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        log.error("Data integrity violation", ex)
        val message = extractConstraintMessage(ex)
        return ResponseEntity(
            ErrorResponse("conflict", message),
            HttpStatus.CONFLICT,
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneral(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error", ex)
        return ResponseEntity(
            ErrorResponse("internal_server_error", "An unexpected error occurred"),
            HttpStatus.INTERNAL_SERVER_ERROR,
        )
    }

    private fun extractConstraintMessage(ex: DataIntegrityViolationException): String {
        val rootCauseMessage = ex.rootCause?.message ?: ex.message ?: "Data conflict"
        return when {
            rootCauseMessage.contains("duplicate key", ignoreCase = true) ||
                rootCauseMessage.contains("unique constraint", ignoreCase = true) ||
                rootCauseMessage.contains("violates unique", ignoreCase = true) -> {
                "A record with the same key already exists"
            }
            rootCauseMessage.contains("foreign key", ignoreCase = true) ||
                rootCauseMessage.contains("violates foreign", ignoreCase = true) -> {
                "Referenced record not found"
            }
            rootCauseMessage.contains("not null", ignoreCase = true) ||
                rootCauseMessage.contains("null value", ignoreCase = true) -> {
                "A required field is missing"
            }
            else -> "Data conflict: $rootCauseMessage"
        }
    }
}
