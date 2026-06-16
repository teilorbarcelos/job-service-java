package com.app.shared.utils;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class AwaitShutdown {
    private AwaitShutdown() {
    }

    public static boolean waitForShutdown(long timeoutMs) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown, "shutdown-listener"));
        return awaitLatch(latch, timeoutMs);
    }

    public static boolean waitForShutdown(Duration timeout) throws InterruptedException {
        if (timeout == null)
            return waitForShutdown(Long.MAX_VALUE);
        return waitForShutdown(timeout.toMillis());
    }

    static boolean awaitLatch(CountDownLatch latch, long timeoutMs) throws InterruptedException {
        return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }
}
