package com.mundial2026.predictions.shared.response;

import java.util.List;

public record ErrorResponse(String code, String message, List<String> details) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }

    public static ErrorResponse of(String code, String message, List<String> details) {
        return new ErrorResponse(code, message, details);
    }
}
