package com.app.core;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

class SchedulerTest {

    private static final Logger LOG = Logger.getLogger(SchedulerTest.class);

    @Test
    void start_and_stop_with_no_jobs() throws Exception {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {
            var scheduler = new Scheduler(List.of(), new CronUtilsAdapter(), Duration.ofSeconds(1), LOG,
                ZonedDateTime::now, exec);
            scheduler.start();
            scheduler.stop();
        } finally {
            exec.shutdownNow();
        }
    }

    @Test
    void start_twice_throws() {
        var scheduler = new Scheduler(List.of(), new CronUtilsAdapter(), Duration.ofSeconds(1), LOG);
        scheduler.start();
        try {
            assertThrows(IllegalStateException.class, scheduler::start);
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void start_after_stop_throws() {
        var scheduler = new Scheduler(List.of(), new CronUtilsAdapter(), Duration.ofSeconds(1), LOG);
        scheduler.start();
        scheduler.stop();
        assertThrows(IllegalStateException.class, scheduler::start);
    }

    @Test
    void stop_is_idempotent() {
        var scheduler = new Scheduler(List.of(), new CronUtilsAdapter(), Duration.ofSeconds(1), LOG);
        scheduler.stop();
        scheduler.stop();
    }

    @Test
    void start_rejects_duplicate_job_names() {
        var jobs = List.<BaseJob>of(
            new TestJob("a", 0, false),
            new TestJob("a", 0, false));
        var scheduler = new Scheduler(jobs, new CronUtilsAdapter(), Duration.ofSeconds(1), LOG);
        var ex = assertThrows(IllegalStateException.class, scheduler::start);
        assertTrue(ex.getMessage().contains("duplicate"));
        scheduler.stop();
    }

    @Test
    void start_rejects_invalid_cron() {
        var jobs = List.<BaseJob>of(new TestJob("bad", 0, false));
        var adapter = new CronUtilsAdapter() {
            @Override public java.util.Optional<CronSchedule> parse(String expression) {
                return java.util.Optional.empty();
            }
        };
        var scheduler = new Scheduler(jobs, adapter, Duration.ofSeconds(1), LOG);
        var ex = assertThrows(IllegalStateException.class, scheduler::start);
        assertTrue(ex.getMessage().contains("invalid cron"));
        scheduler.stop();
    }

    @Test
    void start_skips_disabled_jobs() throws Exception {
        AtomicInteger ran = new AtomicInteger();
        var jobs = List.<BaseJob>of(new TestJob("disabled", 0, false) {
            @Override public boolean enabled() { return false; }
            @Override public void run(JobContext context) { ran.incrementAndGet(); }
        });
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {
            ZonedDateTime fixed = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
            var scheduler = new Scheduler(jobs, new CronUtilsAdapter(), Duration.ofSeconds(1), LOG,
                () -> fixed, exec);
            scheduler.start();
            Thread.sleep(100);
            scheduler.stop();
        } finally {
            exec.shutdownNow();
        }
        assertEquals(0, ran.get());
    }

    @Test
    void job_runs_at_scheduled_time() throws Exception {
        CountDownLatch ran = new CountDownLatch(1);
        // Start near a minute boundary so the next "every minute" cron fires soon
        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneId.of("UTC"))
            .withSecond(59).withNano(0);
        ZonedDateTime start = now.atZone(java.time.ZoneId.of("UTC"));
        var jobs = List.<BaseJob>of(new TestJob("fires", 50, false) {
            @Override public void run(JobContext context) { ran.countDown(); }
        });
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {
            var scheduler = new Scheduler(jobs, new CronUtilsAdapter(), Duration.ofSeconds(1), LOG,
                () -> start, exec);
            scheduler.start();
            assertTrue(ran.await(3, TimeUnit.SECONDS), "job should run at least once");
            scheduler.stop();
        } finally {
            exec.shutdownNow();
        }
    }

    @Test
    void current_time_returns_supplier() {
        ZonedDateTime fixed = ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneId.of("UTC"));
        var scheduler = new Scheduler(List.of(), new CronUtilsAdapter(), Duration.ofSeconds(1), LOG,
            () -> fixed, Executors.newSingleThreadScheduledExecutor());
        assertEquals(Instant.parse("2026-06-15T12:00:00Z"), scheduler.currentTime());
        scheduler.stop();
    }

    @Test
    void is_running_false_when_not_started() {
        var scheduler = new Scheduler(List.of(), new CronUtilsAdapter(), Duration.ofSeconds(1), LOG);
        assertFalse(scheduler.isRunning("any"));
    }

    static class TestJob implements BaseJob {
        private final String name;
        private final long sleepMs;
        private final boolean throwOnRun;

        TestJob(String name, long sleepMs, boolean throwOnRun) {
            this.name = name;
            this.sleepMs = sleepMs;
            this.throwOnRun = throwOnRun;
        }

        @Override public String name() { return name; }
        @Override public String schedule() { return "*/1 * * * *"; }
        @Override public String description() { return "test"; }
        @Override public boolean enabled() { return true; }
        @Override public void run(JobContext context) throws Exception {
            if (throwOnRun) throw new RuntimeException("test failure");
            if (sleepMs > 0) Thread.sleep(sleepMs);
        }
    }
}
