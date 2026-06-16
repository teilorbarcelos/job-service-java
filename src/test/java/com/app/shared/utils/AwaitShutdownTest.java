package com.app.shared.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AwaitShutdownTest {

    @BeforeEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    @AfterEach
    void ensureInterruptCleared() {
        Thread.interrupted();
    }

    @Test
    void waitForShutdown_returns_false_on_timeout() throws Exception {
        boolean signaled = AwaitShutdown.waitForShutdown(Duration.ofMillis(50));
        assertFalse(signaled);
    }

    @Test
    void waitForShutdown_null_timeout_blocks_indefinitely_then_interrupts() {
        Thread.currentThread().interrupt();
        assertThrows(InterruptedException.class, () -> AwaitShutdown.waitForShutdown((Duration) null));
    }

    @Test
    void awaitLatch_returns_true_when_counted_down() throws Exception {
        var latch = new CountDownLatch(1);
        var ready = new CountDownLatch(1);
        var resultRef = new AtomicReference<Boolean>();
        Thread t = new Thread(() -> {
            try {
                ready.countDown();
                resultRef.set(AwaitShutdown.awaitLatch(latch, 5000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.start();
        ready.await(500, TimeUnit.MILLISECONDS);
        latch.countDown();
        t.join(2000);
        assertTrue(resultRef.get());
    }

    @Test
    void awaitLatch_returns_false_on_timeout() throws Exception {
        var latch = new CountDownLatch(1);
        assertFalse(AwaitShutdown.awaitLatch(latch, 10));
    }
}
