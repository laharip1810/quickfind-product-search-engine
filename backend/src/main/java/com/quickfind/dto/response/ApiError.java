package com.quickfind.dto.response;

import java.time.Instant;
import java.util.List;

/** The single error shape returned by every endpoint. Never contains stack traces. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors) {

    public record FieldErrorDetail(String field, String message) {
    }
}
