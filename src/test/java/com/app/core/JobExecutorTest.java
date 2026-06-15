package com.app.core;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

class JobExecutorTest {

    private static final Logger LOG = Logger.getLogger(JobExecutorTest.class);

    @Test
    void execute_succeeds() {
        var job = new TestJob("ok", false, null, 0, false);
        var ctx = new JobContext(LOG);
        var result = JobExecutor.execute(job, ctx, Duration.ofSeconds(1));
        assertEquals("ok", result.job());
        assertEquals(JobStatus.SUCCESS, result.status());
        assertNull(result.error());
        assertTrue(result.durationMs() >= 0);
        assertTrue(job.ran.get());
    }

    @Test
    void execute_fails_on_exception() {
        var job = new TestJob("boom", true, new RuntimeException("kaboom"), 0, false);
        var ctx = new JobContext(LOG);
        var result = JobExecutor.execute(job, ctx, Duration.ofSeconds(1));
        assertEquals(JobStatus.FAILED, result.status());
        assertEquals("kaboom", result.error());
    }

    @Test
    void execute_times_out() {
        var job = new TestJob("slow", false, null, 5_000, false);
        var ctx = new JobContext(LOG);
        var result = JobExecutor.execute(job, ctx, Duration.ofMillis(50));
        assertEquals(JobStatus.TIMEOUT, result.status());
    }

    @Test
    void execute_succeeds_with_null_timeout() {
        var job = new TestJob("no-timeout", false, null, 0, false);
        var ctx = new JobContext(LOG);
        var result = JobExecutor.execute(job, ctx, null);
        assertEquals(JobStatus.SUCCESS, result.status());
    }

    @Test
    void execute_interrupted_returns_cancelled() {
        var job = new TestJob("interruptible", false, null, 0, true);
        var ctx = new JobContext(LOG);
        var result = JobExecutor.execute(job, ctx, Duration.ofSeconds(2));
        assertEquals(JobStatus.CANCELLED, result.status());
    }

    static class TestJob implements BaseJob {
        private final String name;
        private final boolean throwOnRun;
        private final RuntimeException toThrow;
        private final long sleepMs;
        private final boolean interruptible;
        final AtomicBoolean ran = new AtomicBoolean(false);

        TestJob(String name, boolean throwOnRun, RuntimeException toThrow, long sleepMs, boolean interruptible) {
            this.name = name;
            this.throwOnRun = throwOnRun;
            this.toThrow = toThrow;
            this.sleepMs = sleepMs;
            this.interruptible = interruptible;
        }

        @Override public String name() { return name; }
        @Override public String schedule() { return "* * * * *"; }
        @Override public String description() { return "test"; }
        @Override public void run(JobContext context) throws Exception {
            ran.set(true);
            if (throwOnRun) throw toThrow;
            if (sleepMs > 0) Thread.sleep(sleepMs);
            if (interruptible) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("test interrupt");
            }
        }
    }
}
