package com.app.core;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

public class Scheduler implements AutoCloseable {

    private final List<BaseJob> jobs;
    private final CronAdapter cronAdapter;
    private final Duration timeout;
    private final Logger logger;
    private final Supplier<ZonedDateTime> nowSupplier;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final Map<String, ScheduledFuture<?>> futures = new HashMap<>();
    private final Map<String, AtomicBoolean> running = new HashMap<>();

    public Scheduler(List<BaseJob> jobs, CronAdapter cronAdapter, Duration timeout, Logger logger) {
        this(jobs, cronAdapter, timeout, logger, () -> ZonedDateTime.now(ZoneId.of("UTC")),
             Executors.newScheduledThreadPool(Math.max(1, jobs.size())));
    }

    Scheduler(List<BaseJob> jobs, CronAdapter cronAdapter, Duration timeout, Logger logger,
              Supplier<ZonedDateTime> nowSupplier, ScheduledExecutorService executor) {
        this.jobs = jobs;
        this.cronAdapter = cronAdapter;
        this.timeout = timeout;
        this.logger = logger;
        this.nowSupplier = nowSupplier;
        this.executor = executor;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("scheduler already started");
        }
        if (stopped.get()) {
            throw new IllegalStateException("scheduler has been stopped");
        }
        checkDuplicates();
        Map<String, CronSchedule> schedules = parseSchedules();
        for (BaseJob job : jobs) {
            if (!job.enabled()) continue;
            CronSchedule schedule = schedules.get(job.name());
            scheduleNext(job, schedule);
        }
    }

    private void checkDuplicates() {
        Map<String, Integer> counts = new HashMap<>();
        for (BaseJob job : jobs) {
            counts.merge(job.name(), 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > 1) {
                throw new IllegalStateException("duplicate job name: " + e.getKey());
            }
        }
    }

    private Map<String, CronSchedule> parseSchedules() {
        Map<String, CronSchedule> result = new HashMap<>();
        for (BaseJob job : jobs) {
            if (!job.enabled()) continue;
            Optional<CronSchedule> parsed = cronAdapter.parse(job.schedule());
            if (parsed.isEmpty()) {
                throw new IllegalStateException("invalid cron expression for job '" + job.name() + "': " + job.schedule());
            }
            result.put(job.name(), parsed.get());
        }
        return result;
    }

    private void scheduleNext(BaseJob job, CronSchedule schedule) {
        ZonedDateTime now = nowSupplier.get();
        ZonedDateTime next = schedule.next(now);
        long delayMs = Math.max(0, Duration.between(now, next).toMillis());
        logger.infof("Job %s scheduled to run in %dms (at %s)", job.name(), delayMs, next);

        ScheduledFuture<?> future = executor.schedule(() -> runAndReschedule(job, schedule), delayMs, TimeUnit.MILLISECONDS);
        futures.put(job.name(), future);
    }

    private void runAndReschedule(BaseJob job, CronSchedule schedule) {
        if (stopped.get()) return;
        AtomicBoolean flag = running.computeIfAbsent(job.name(), k -> new AtomicBoolean(false));
        if (!flag.compareAndSet(false, true)) {
            logger.warnf("Job %s still running, skipping this tick", job.name());
        } else {
            try {
                JobContext context = new JobContext(logger);
                JobResult result = JobExecutor.execute(job, context, timeout);
                logger.debugf("Job %s finished with status %s", job.name(), result.status());
            } finally {
                flag.set(false);
            }
        }
        if (!stopped.get()) {
            try {
                scheduleNext(job, schedule);
            } catch (Exception e) {
                logger.errorf(e, "Failed to reschedule job %s", job.name());
            }
        }
    }

    public void stop() {
        if (!stopped.compareAndSet(false, true)) return;
        started.set(false);
        for (ScheduledFuture<?> f : futures.values()) {
            f.cancel(false);
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    @Override
    public void close() {
        stop();
    }

    public boolean isRunning(String jobName) {
        AtomicBoolean flag = running.get(jobName);
        return flag != null && flag.get();
    }

    public Instant currentTime() {
        return nowSupplier.get().toInstant();
    }
}
