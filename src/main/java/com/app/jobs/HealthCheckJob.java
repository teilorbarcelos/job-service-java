package com.app.jobs;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.app.core.BaseJob;
import com.app.core.JobContext;
import com.app.infrastructure.health.HealthCheckResult;
import com.app.infrastructure.health.HealthChecker;
import com.app.shared.config.AppSettings;

public class HealthCheckJob implements BaseJob {

    private final String cron;
    private final boolean enabled;
    private final HealthChecker checker;

    public HealthCheckJob(HealthChecker checker, AppSettings settings) {
        this.checker = checker;
        String c = settings.healthCheckCron();
        this.cron = (c == null || c.isBlank()) ? "*/1 * * * *" : c;
        this.enabled = settings.healthCheckEnabled();
    }

    @Override
    public String name() { return "health-check"; }

    @Override
    public String schedule() { return cron; }

    @Override
    public String description() {
        return "Reports connection status with PostgreSQL, Redis and RabbitMQ";
    }

    @Override
    public boolean enabled() { return enabled; }

    @Override
    public void run(JobContext context) {
        HealthCheckResult pg = checker.checkPostgres();
        HealthCheckResult rd = checker.checkRedis();
        HealthCheckResult rb = checker.checkRabbit();
        context.logger().infof("Health check: postgres=%s redis=%s rabbit=%s",
            pg.status(), rd.status(), rb.status());
        String ts = DateTimeFormatter.ISO_INSTANT.format(Instant.now().atOffset(ZoneOffset.UTC));
        System.out.printf("[HealthCheck %s] postgres=%s redis=%s rabbitmq=%s%n",
            ts, pg.status(), rd.status(), rb.status());
    }
}
