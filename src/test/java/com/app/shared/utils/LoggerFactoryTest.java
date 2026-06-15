package com.app.shared.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.app.shared.errors.AppError;

class LoggerFactoryTest {

    @Test
    void create_returns_logger_for_name() {
        var logger = LoggerFactory.create("test-logger");
        assertNotNull(logger);
        assertEquals("test-logger", logger.getName());
    }

    @Test
    void create_is_idempotent() {
        var l1 = LoggerFactory.create("same-name");
        var l2 = LoggerFactory.create("same-name");
        assertSame(l1, l2);
    }
}
