package com.digitalcertificate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/** Standard envelope for every REST API response. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data, LocalDateTime timestamp) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
}
