package com.app.shared.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.app.shared.errors.AppError;

class AppSettingsTest {

    private static UnaryOperator<String> env(String... pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map::get;
    }

    private static UnaryOperator<String> empty() {
        return k -> null;
    }

    @Test
    void load_defaults() {
        var s = AppSettings.load(empty());
        assertEquals("local", s.environment());
        assertEquals("INFO", s.logLevel());
        assertEquals(Duration.ofSeconds(30), s.shutdownTimeout());
        assertEquals(Duration.ofSeconds(300), s.jobExecutionTimeout());
        assertEquals("*/1 * * * *", s.healthCheckCron());
        assertTrue(s.healthCheckEnabled());
        assertFalse(s.messagingEnabled());
        assertEquals(6379, s.redisPort());
    }

    @Test
    void load_overrides() {
        var s = AppSettings.load(env(
            "ENVIRONMENT", "prod",
            "JOB_EXECUTION_TIMEOUT_SECONDS", "60",
            "MESSAGING_ENABLED", "true"
        ));
        assertEquals("prod", s.environment());
        assertEquals(Duration.ofSeconds(60), s.jobExecutionTimeout());
        assertTrue(s.messagingEnabled());
    }

    @Test
    void load_blank_falls_back_to_default() {
        var s = AppSettings.load(env("ENVIRONMENT", ""));
        assertEquals("local", s.environment());
    }

    @Test
    void load_invalid_int_throws() {
        assertThrows(AppError.class, () -> AppSettings.load(env(
            "JOB_EXECUTION_TIMEOUT_SECONDS", "not-a-number"
        )));
    }

    @Test
    void load_invalid_bool_throws() {
        assertThrows(AppError.class, () -> AppSettings.load(env(
            "MESSAGING_ENABLED", "maybe"
        )));
    }

    @Test
    void load_bool_truthy_variants() {
        for (String truthy : new String[]{"true", "True", "1"}) {
            var s = AppSettings.load(env("MESSAGING_ENABLED", truthy));
            assertTrue(s.messagingEnabled(), "expected true for '" + truthy + "'");
        }
    }

    @Test
    void load_bool_falsy_variants() {
        for (String falsy : new String[]{"false", "False", "0"}) {
            var s = AppSettings.load(env("MESSAGING_ENABLED", falsy));
            assertFalse(s.messagingEnabled(), "expected false for '" + falsy + "'");
        }
    }

    @Test
    void load_falls_back_for_missing() {
        var s = AppSettings.load(env("REDIS_HOST", null));
        assertEquals("localhost", s.redisHost());
    }

    @Test
    void load_load_uses_system_env_when_no_supplier() {
        // Smoke test: load() without args must not throw
        assertDoesNotThrow(() -> AppSettings.load());
    }
}
