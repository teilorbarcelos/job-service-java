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
        var job = new TestJob("ok", null, 0, false);
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
        var job = new TestJob("boom", new RuntimeException("kaboom"), 0, false);
        var ctx = new JobContext(LOG);
        var result = JobExecutor.execute(job, ctx, Duration.ofSeconds(1));
        assertEquals(JobStatus.FAILED, result.status());
        assertEquals("kaboom", result.error());
    }

    @Test
    void execute_fails_on_error() {
        var job = new TestJob("err", new java.lang.Error("oops"), 0, false);
        var ctx = new JobContext(LOG);
        var result = JobExecutor.execute(job, ctx, Duration.ofSeconds(1));
        assertEquals(JobStatus.FAILED, result.status());
        assertTrue(result.error().contains("oops"));
    }

    @Test
    void execute_times_out() {
        var job = new TestJob("slow", null, 5_000, false);
        var ctx = new JobContext(LOG);
        var result = JobExecutor.execute(job, ctx, Duration.ofMillis(50));
        assertEquals(JobStatus.TIMEOUT, result.status());
    }

    @Test
    void execute_succeeds_with_null_timeout() {
        var job = new TestJob("no-timeout", null, 0, false);
        var ctx = new JobContext(LOG);
        var result = JobExecutor.execute(job, ctx, null);
        assertEquals(JobStatus.SUCCESS, result.status());
    }

    @Test
    void execute_succeeds_with_zero_timeout() {
        var job = new TestJob("zero", null, 0, false);
        var ctx = new JobContext(LOG);
        var result = JobExecutor.execute(job, ctx, Duration.ZERO);
        assertEquals(JobStatus.SUCCESS, result.status());
    }

    @Test
    void execute_interrupted_returns_cancelled() {
        var job = new TestJob("interruptible", null, 0, true);
        var ctx = new JobContext(LOG);
        var result = JobExecutor.execute(job, ctx, Duration.ofSeconds(2));
        assertEquals(JobStatus.CANCELLED, result.status());
    }

    @Test
    void execute_null_timeout_doesnt_apply_timeout_path() {
        var job = new TestJob("with-null", null, 0, false);
        var ctx = new JobContext(LOG);
        var result = JobExecutor.execute(job, ctx, null);
        assertEquals(JobStatus.SUCCESS, result.status());
        assertTrue(result.durationMs() >= 0);
    }

    static class TestJob implements BaseJob {
        private final String name;
        private final Throwable toThrow;
        private final long sleepMs;
        private final boolean interruptible;
        final AtomicBoolean ran = new AtomicBoolean(false);

        TestJob(String name, Throwable toThrow, long sleepMs, boolean interruptible) {
            this.name = name;
            this.toThrow = toThrow;
            this.sleepMs = sleepMs;
            this.interruptible = interruptible;
        }

        @Override public String name() { return name; }
        @Override public String schedule() { return "* * * * *"; }
        @Override public String description() { return "test"; }
        @Override public void run(JobContext context) throws Exception {
            ran.set(true);
            if (toThrow != null) {
                if (toThrow instanceof Exception e) throw e;
                throw (Error) toThrow;
            }
            if (sleepMs > 0) Thread.sleep(sleepMs);
            if (interruptible) {
                Thread.currentThread().interrupt();
                throw new InterruptedException("test interrupt");
            }
        }
    }
}
