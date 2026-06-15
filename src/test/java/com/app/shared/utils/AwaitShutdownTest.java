package com.app.shared.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class AwaitShutdownTest {

    @Test
    void waitForShutdown_returns_false_on_timeout() throws Exception {
        boolean signaled = AwaitShutdown.waitForShutdown(Duration.ofMillis(50));
        assertFalse(signaled);
    }

    @Test
    void waitForShutdown_null_timeout_blocks_indefinitely_then_interrupts() {
        // Verify the null-overload delegates to the MAX_VALUE form by
        // interrupting the calling thread before the call.
        Thread.currentThread().interrupt();
        assertThrows(InterruptedException.class, () ->
            AwaitShutdown.waitForShutdown((Duration) null));
    }
}
