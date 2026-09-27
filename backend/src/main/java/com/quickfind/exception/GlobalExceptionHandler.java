package com.quickfind.exception;

import com.quickfind.dto.response.ApiError;
import com.quickfind.dto.response.ApiError.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts every exception into the same JSON shape ({@link ApiError}).
 * Expected errors are logged at WARN without a stack trace; unexpected ones at ERROR
 * with the stack trace in the server log only. Stack traces never reach the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(QuickFindException.class)
    public ResponseEntity<ApiError> handleQuickFind(QuickFindException ex, HttpServletRequest request) {
        log.warn("{} on {} {}: {}", ex.getErrorCode(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(ex.getErrorCode(), ex.getMessage(), request, List.of());
    }

    /** @Valid @RequestBody failures (MethodArgumentNotValidException) and query-param binding failures. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleBind(BindException ex, HttpServletRequest request) {
        List<FieldErrorDetail> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDetail(fe.getField(), fieldMessage(fe.getField(), fe.getDefaultMessage(), fe.isBindingFailure())))
                .toList();
        List<FieldErrorDetail> globals = ex.getBindingResult().getGlobalErrors().stream()
                .map(ge -> new FieldErrorDetail(ge.getObjectName(), ge.getDefaultMessage()))
                .toList();
        List<FieldErrorDetail> all = new ArrayList<>(fields);
        all.addAll(globals);
        log.warn("Validation failed on {} {}: {}", request.getMethod(), request.getRequestURI(), all);
        return build(ErrorCode.VALIDATION_FAILED, "Request validation failed", request, all);
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<FieldErrorDetail> fields = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorDetail(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, "Request validation failed", request, fields);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidation(ValidationException ex, HttpServletRequest request) {
        return build(ErrorCode.VALIDATION_FAILED, "Request validation failed", request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Parameter '" + ex.getName() + "' has an invalid value '" + ex.getValue() + "'";
        return build(ErrorCode.INVALID_REQUEST, message, request, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(ErrorCode.INVALID_REQUEST, "Missing required parameter '" + ex.getParameterName() + "'", request, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(ErrorCode.INVALID_REQUEST,
                "Malformed JSON body or a field has the wrong type (enum values are case-sensitive)", request, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return build(ErrorCode.INVALID_REQUEST, "Content-Type must be application/json", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, "Method " + ex.getMethod() + " is not supported here", request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, "No endpoint " + request.getMethod() + " " + request.getRequestURI(), request, List.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Optimistic lock conflict on {}", request.getRequestURI());
        return build(ErrorCode.VERSION_CONFLICT, "The product was modified concurrently. Reload it and try again.", request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation on {} {}: {}", request.getMethod(), request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.DATA_CONFLICT, "The request conflicts with existing data", request, List.of());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        log.error("Database error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(ErrorCode.DATABASE_ERROR, "The database is temporarily unavailable. Please retry.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request, List.of());
    }

    private static String fieldMessage(String field, String defaultMessage, boolean bindingFailure) {
        // Binding failures (e.g. minPrice=abc) carry a long technical message; replace it.
        return bindingFailure ? "'" + field + "' has an invalid value" : defaultMessage;
    }

    private static ResponseEntity<ApiError> build(ErrorCode code, String message, HttpServletRequest request,
                                                  List<FieldErrorDetail> fieldErrors) {
        ApiError body = new ApiError(Instant.now(), code.status().value(), code.name(), message,
                request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(code.status()).body(body);
    }
}
