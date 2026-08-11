package com.apptitle.common.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform error body returned by every failed API call.
 * Kept as a plain record so every controller/exception handler returns
 * the exact same shape the frontend's api client can rely on.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ApiErrorResponse withFieldErrors(int status, String error, String message, String path,
                                                     Map<String, String> fieldErrors) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, fieldErrors);
    }
}
