package com.app.core;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.logging.Logger;

public final class JobExecutor {
    private JobExecutor() {}

    public static JobResult execute(BaseJob job, JobContext context, Duration timeout) {
        Instant start = Instant.now();
        Logger logger = context.logger();
        long timeoutMs = timeout == null ? 0 : timeout.toMillis();
        try {
            if (timeoutMs > 0) {
                executeWithTimeout(job, context, timeoutMs);
                long durationMs = Duration.between(start, Instant.now()).toMillis();
                logger.infof("Job %s completed in %dms", job.name(), durationMs);
                return new JobResult(job.name(), JobStatus.SUCCESS, durationMs, null);
            } else {
                job.run(context);
                long durationMs = Duration.between(start, Instant.now()).toMillis();
                logger.infof("Job %s completed in %dms", job.name(), durationMs);
                return new JobResult(job.name(), JobStatus.SUCCESS, durationMs, null);
            }
        } catch (TimeoutException e) {
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            logger.warnf("Job %s exceeded timeout of %dms", job.name(), timeoutMs);
            return new JobResult(job.name(), JobStatus.TIMEOUT, durationMs, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            logger.warnf("Job %s interrupted after %dms", job.name(), durationMs);
            return new JobResult(job.name(), JobStatus.CANCELLED, durationMs, e.getMessage());
        } catch (Exception e) {
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            logger.errorf(e, "Job %s failed after %dms", job.name(), durationMs);
            return new JobResult(job.name(), JobStatus.FAILED, durationMs, e.getMessage());
        }
    }

    private static void executeWithTimeout(BaseJob job, JobContext context, long timeoutMs)
            throws Exception {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                job.run(context);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException && cause.getCause() instanceof Exception ex) {
                throw ex;
            }
            if (cause instanceof Exception ex) {
                throw ex;
            }
            throw new RuntimeException(cause);
        } finally {
            future.cancel(true);
        }
    }
}
