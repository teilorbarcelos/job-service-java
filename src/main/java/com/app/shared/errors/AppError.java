package com.app.shared.errors;

public class AppError extends RuntimeException {
    private final String code;
    private final int statusCode;

    public AppError(String code, String message, int statusCode) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
    }

    public AppError(String code, String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.statusCode = statusCode;
    }

    public String getCode() { return code; }
    public int getStatusCode() { return statusCode; }

    public static AppError configuration(String message) {
        return new AppError("CONFIGURATION_ERROR", message, 500);
    }

    public static AppError validation(String message) {
        return new AppError("VALIDATION_ERROR", message, 400);
    }

    public static AppError connection(String service, String message) {
        return new AppError("CONNECTION_ERROR", service + ": " + message, 503);
    }
}
