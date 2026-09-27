package com.quickfind.exception;

import org.springframework.http.HttpStatus;

/** Machine-readable error codes returned in the "error" field of every error response. */
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    INVALID_SEARCH_REQUEST(HttpStatus.BAD_REQUEST),
    INVALID_SORT(HttpStatus.BAD_REQUEST),
    INVALID_FILTER(HttpStatus.BAD_REQUEST),
    INVALID_PRODUCT(HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    DUPLICATE_PRODUCT(HttpStatus.CONFLICT),
    VERSION_CONFLICT(HttpStatus.CONFLICT),
    DATA_CONFLICT(HttpStatus.CONFLICT),
    DATABASE_ERROR(HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
