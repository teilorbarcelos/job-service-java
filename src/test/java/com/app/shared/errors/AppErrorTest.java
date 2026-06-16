package com.app.shared.errors;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AppErrorTest {

    @Test
    void stores_code_message_status() {
        var err = new AppError("X", "boom", 418);
        assertEquals("X", err.getCode());
        assertEquals("boom", err.getMessage());
        assertEquals(418, err.getStatusCode());
    }

    @Test
    void with_cause_chains_message() {
        var cause = new RuntimeException("root");
        var err = new AppError("X", "wrapped", 500, cause);
        assertEquals("wrapped: root", err.getMessage());
        assertSame(cause, err.getCause());
    }

    @Test
    void configuration_error() {
        var err = AppError.configuration("missing env");
        assertEquals("CONFIGURATION_ERROR", err.getCode());
        assertEquals(500, err.getStatusCode());
    }

    @Test
    void validation_error() {
        var err = AppError.validation("bad input");
        assertEquals("VALIDATION_ERROR", err.getCode());
        assertEquals(400, err.getStatusCode());
    }

    @Test
    void connection_error_prefixes_service_name() {
        var err = AppError.connection("RabbitMQ", "timeout");
        assertEquals("CONNECTION_ERROR", err.getCode());
        assertEquals(503, err.getStatusCode());
        assertTrue(err.getMessage().contains("RabbitMQ"));
        assertTrue(err.getMessage().contains("timeout"));
    }
}
