package com.backoffice.pos.common.web;

import java.time.Instant;
import java.util.List;

/** Standard error response envelope. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }
}
