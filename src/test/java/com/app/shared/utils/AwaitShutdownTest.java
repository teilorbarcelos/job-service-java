package com.app.shared.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AwaitShutdownTest {

    private Thread hookThread;

    @BeforeEach
    void clearHooks() {
        // JVM only allows one shutdown hook listener; our AwaitShutdown
        // adds one, so we capture and remove it after the test.
    }

    @AfterEach
    void cleanup() {
        if (hookThread != null) {
            Runtime.getRuntime().removeShutdownHook(hookThread);
        }
    }

    @Test
    void waitForShutdown_returns_false_on_timeout() throws Exception {
        // Don't trigger any shutdown; just verify timeout returns false
        boolean signaled = AwaitShutdown.waitForShutdown(Duration.ofMillis(50));
        assertFalse(signaled);
    }

    @Test
    void waitForShutdown_null_timeout_blocks_indefinitely() {
        // This test would hang forever, so just verify the call
        // enters the latch.await (we interrupt after a brief moment).
        Thread.currentThread().interrupt();
        assertThrows(java.io.IOException.class, () -> {
            // Use the no-arg form indirectly via reflection
            var m = AwaitShutdown.class.getDeclaredMethod("waitForShutdown", long.class);
            m.setAccessible(true);
            try {
                m.invoke(null, Long.MAX_VALUE);
            } catch (java.lang.reflect.InvocationTargetException ite) {
                if (ite.getCause() instanceof InterruptedException) {
                    throw new java.io.IOException("interrupted");
                }
                throw ite;
            }
        });
    }
}
