package com.example.visited.globalexceptionhandler;

import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.CannotCreateTransactionException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Standard error response structure
    record ApiError(
            String timestamp,
            int status,
            String error,
            String message,
            String path,
            Map<String, String> details // nullable
    ) {
        public ApiError(int status, String error, String message, String path, Map<String, String> details) {
            this(LocalDateTime.now().toString(), status, error, message, path, details);
        }

        public ApiError(int status, String error, String message, String path) {
            this(status, error, message, path, null);
        }
    }

    // Helper method
    private ResponseEntity<ApiError> createErrorResponse(
            HttpServletRequest request,
            HttpStatus status,
            String errorType,
            String message,
            Map<String, String> details) {

        ApiError error = new ApiError(
                status.value(),
                errorType,
                message,
                request.getRequestURI(),
                details
        );

        return new ResponseEntity<>(error, status);
    }

    // ───────────────────────────────────────────────────────────────
    //  1. Validation errors (@Valid, @Validated)
    // ───────────────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationError(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return createErrorResponse(
                request,
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Validation failed",
                fieldErrors
        );
    }

    // ───────────────────────────────────────────────────────────────
    //  2. Security related (AccessDenied, your SecurityException)
    // ───────────────────────────────────────────────────────────────
    @ExceptionHandler({AccessDeniedException.class, SecurityException.class})
    public ResponseEntity<ApiError> handleSecurityExceptions(
            Exception ex,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.FORBIDDEN;
        String errorCode = "FORBIDDEN";
        String message = ex.getMessage() != null ? ex.getMessage() : "Access denied";

        if (ex instanceof SecurityException) {
            status = HttpStatus.UNAUTHORIZED;
            errorCode = "UNAUTHORIZED";
        }

        return createErrorResponse(request, status, errorCode, message, null);
    }

    // ───────────────────────────────────────────────────────────────
    //  3. Business logic errors (most common in your controllers)
    // ───────────────────────────────────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        return createErrorResponse(
                request,
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                ex.getMessage(),
                null
        );
    }

    // ───────────────────────────────────────────────────────────────
    //  4. Catch-all - unknown/unexpected errors (MUST HAVE)
    // ───────────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllOtherExceptions(
            Exception ex,
            HttpServletRequest request) {

        // IMPORTANT: Always log the real exception!
        log.error("Unhandled exception occurred", ex);

        return createErrorResponse(
                request,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later.",
                null
        );
    }
 // 1. Better handling for "not found" cases
    @ExceptionHandler({NoResultException.class, EntityNotFoundException.class, 
                       org.springframework.dao.EmptyResultDataAccessException.class})
    public ResponseEntity<ApiError> handleNotFound(Exception ex, HttpServletRequest request) {
        return createErrorResponse(
                request,
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                ex.getMessage() != null ? ex.getMessage() : "Resource not found",
                null
        );
    }

    // 2. Optional: Database unique constraint violation → 409 Conflict
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException ex, 
            HttpServletRequest request) {
        return createErrorResponse(
                request,
                HttpStatus.CONFLICT,
                "CONFLICT",
                "Resource already exists or constraint violation",
                null
        );
    }

    // ───────────────────────────────────────────────────────────────
    //  DATABASE EXCEPTIONS
    // ───────────────────────────────────────────────────────────────
    @ExceptionHandler({SQLException.class, CannotCreateTransactionException.class})
    public ResponseEntity<ApiError> handleDatabaseException(
            Exception ex,
            HttpServletRequest request) {
        log.error("Database error occurred", ex);
        return createErrorResponse(
                request,
                HttpStatus.SERVICE_UNAVAILABLE,
                "DATABASE_ERROR",
                "Database temporarily unavailable. Please try again later.",
                null
        );
    }

    // ───────────────────────────────────────────────────────────────
    //  TRANSACTION EXCEPTIONS
    // ───────────────────────────────────────────────────────────────
    @ExceptionHandler({TransactionTimedOutException.class, QueryTimeoutException.class})
    public ResponseEntity<ApiError> handleTransactionTimeout(
            Exception ex,
            HttpServletRequest request) {
        log.warn("Transaction timeout: {}", ex.getMessage());
        return createErrorResponse(
                request,
                HttpStatus.REQUEST_TIMEOUT,
                "TIMEOUT",
                "Request took too long to process. Please try again.",
                null
        );
    }

    @ExceptionHandler({OptimisticLockException.class, PessimisticLockException.class, CannotAcquireLockException.class})
    public ResponseEntity<ApiError> handleConcurrentUpdate(
            Exception ex,
            HttpServletRequest request) {
        log.warn("Concurrent update conflict: {}", ex.getMessage());
        return createErrorResponse(
                request,
                HttpStatus.CONFLICT,
                "CONCURRENT_UPDATE",
                "Resource was modified by another user. Please refresh and try again.",
                null
        );
    }

    // ───────────────────────────────────────────────────────────────
    //  HTTP CLIENT EXCEPTIONS
    // ───────────────────────────────────────────────────────────────
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        return createErrorResponse(
                request,
                HttpStatus.BAD_REQUEST,
                "MALFORMED_JSON",
                "Invalid JSON format. Please check your request body.",
                null
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        return createErrorResponse(
                request,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                "Content-Type not supported. Use application/json.",
                null
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        return createErrorResponse(
                request,
                HttpStatus.BAD_REQUEST,
                "MISSING_PARAMETER",
                "Required parameter '" + ex.getParameterName() + "' is missing.",
                null
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        String message = String.format("Parameter '%s' should be of type %s",
                ex.getName(), ex.getRequiredType().getSimpleName());
        return createErrorResponse(
                request,
                HttpStatus.BAD_REQUEST,
                "TYPE_MISMATCH",
                message,
                null
        );
    }

    // ───────────────────────────────────────────────────────────────
    //  NETWORK EXCEPTIONS
    // ───────────────────────────────────────────────────────────────
    @ExceptionHandler({ConnectException.class, SocketTimeoutException.class, UnknownHostException.class})
    public ResponseEntity<ApiError> handleNetworkException(
            Exception ex,
            HttpServletRequest request) {
        log.error("Network error occurred", ex);
        return createErrorResponse(
                request,
                HttpStatus.SERVICE_UNAVAILABLE,
                "NETWORK_ERROR",
                "Network connection failed. Please check your connection and try again.",
                null
        );
    }

    // ───────────────────────────────────────────────────────────────
    //  MEMORY/RESOURCE EXCEPTIONS
    // ───────────────────────────────────────────────────────────────
    @ExceptionHandler({OutOfMemoryError.class, StackOverflowError.class})
    public ResponseEntity<ApiError> handleMemoryError(
            Error ex,
            HttpServletRequest request) {
        log.error("CRITICAL: Memory error occurred", ex);
        return createErrorResponse(
                request,
                HttpStatus.SERVICE_UNAVAILABLE,
                "MEMORY_ERROR",
                "Server is overloaded. Please try again later.",
                null
        );
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiError> handleIOException(
            IOException ex,
            HttpServletRequest request) {
        log.error("IO error occurred", ex);
        return createErrorResponse(
                request,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "IO_ERROR",
                "File or stream operation failed. Please try again.",
                null
        );
    }
}