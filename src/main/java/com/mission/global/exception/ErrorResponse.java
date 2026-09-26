package com.mission.global.exception;

import java.util.List;

public record ErrorResponse(
        int status,
        String code,
        String message,
        List<FieldErrorDetail> errors
) {
    public record FieldErrorDetail(String field, String reason) {}

    public static ErrorResponse of(ErrorCode code) {
        return new ErrorResponse(code.getStatus().value(), code.name(), code.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode code, List<FieldErrorDetail> errors) {
        return new ErrorResponse(code.getStatus().value(), code.name(), code.getMessage(), errors);
    }
}