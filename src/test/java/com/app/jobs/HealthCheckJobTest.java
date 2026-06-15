package com.app.jobs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.app.core.JobContext;
import com.app.infrastructure.health.HealthCheckResult;
import com.app.infrastructure.health.HealthChecker;
import com.app.infrastructure.health.HealthStatus;
import com.app.shared.config.AppSettings;

class HealthCheckJobTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private PrintStream original;

    @BeforeEach
    void captureStdout() {
        original = System.out;
        System.setOut(new PrintStream(out));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(original);
    }

    @Test
    void job_metadata() {
        var checker = mock(HealthChecker.class);
        var settings = defaultSettings("*/5 * * * *", true);
        var job = new HealthCheckJob(checker, settings);
        assertEquals("health-check", job.name());
        assertEquals("*/5 * * * *", job.schedule());
        assertTrue(job.description().contains("PostgreSQL"));
        assertTrue(job.description().contains("Redis"));
        assertTrue(job.description().contains("RabbitMQ"));
        assertTrue(job.enabled());
    }

    @Test
    void job_default_cron_when_empty() {
        var checker = mock(HealthChecker.class);
        var settings = new AppSettings("test", "INFO", Duration.ofSeconds(5), Duration.ofSeconds(5),
            "", Duration.ofSeconds(5), "", "h", 6379, "", 0, Duration.ofSeconds(5),
            false, "u", "u", "u", Duration.ofSeconds(5), "", false);
        var job = new HealthCheckJob(checker, settings);
        assertEquals("*/1 * * * *", job.schedule());
    }

    @Test
    void job_disabled_via_settings() {
        var checker = mock(HealthChecker.class);
        var settings = defaultSettings("*/1 * * * *", false);
        var job = new HealthCheckJob(checker, settings);
        assertFalse(job.enabled());
    }

    @Test
    void run_prints_status_line_when_all_up() {
        var checker = mock(HealthChecker.class);
        when(checker.checkPostgres()).thenReturn(HealthCheckResult.up(5));
        when(checker.checkRedis()).thenReturn(HealthCheckResult.up(1));
        when(checker.checkRabbit()).thenReturn(HealthCheckResult.up(0));

        var job = new HealthCheckJob(checker, defaultSettings("*/1 * * * *", true));
        job.run(new JobContext(Logger.getLogger("test")));

        var output = out.toString();
        assertTrue(output.contains("postgres=UP"));
        assertTrue(output.contains("redis=UP"));
        assertTrue(output.contains("rabbitmq=UP"));
    }

    @Test
    void run_prints_status_when_pg_down() {
        var checker = mock(HealthChecker.class);
        when(checker.checkPostgres()).thenReturn(HealthCheckResult.down(0, "conn refused"));
        when(checker.checkRedis()).thenReturn(HealthCheckResult.up(0));
        when(checker.checkRabbit()).thenReturn(HealthCheckResult.disabled());

        var job = new HealthCheckJob(checker, defaultSettings("*/1 * * * *", true));
        job.run(new JobContext(Logger.getLogger("test")));

        assertTrue(out.toString().contains("postgres=DOWN"));
    }

    @Test
    void run_prints_status_when_redis_disabled() {
        var checker = mock(HealthChecker.class);
        when(checker.checkPostgres()).thenReturn(HealthCheckResult.up(0));
        when(checker.checkRedis()).thenReturn(HealthCheckResult.up(0));
        when(checker.checkRabbit()).thenReturn(HealthCheckResult.disabled());

        var job = new HealthCheckJob(checker, defaultSettings("*/1 * * * *", true));
        job.run(new JobContext(Logger.getLogger("test")));

        assertTrue(out.toString().contains("rabbitmq=DISABLED"));
    }

    @Test
    void run_prints_status_when_rabbit_down() {
        var checker = mock(HealthChecker.class);
        when(checker.checkPostgres()).thenReturn(HealthCheckResult.up(0));
        when(checker.checkRedis()).thenReturn(HealthCheckResult.up(0));
        when(checker.checkRabbit()).thenReturn(HealthCheckResult.down(0, "closed"));

        var job = new HealthCheckJob(checker, defaultSettings("*/1 * * * *", true));
        job.run(new JobContext(Logger.getLogger("test")));

        assertTrue(out.toString().contains("rabbitmq=DOWN"));
    }

    private static AppSettings defaultSettings(String cron, boolean enabled) {
        return new AppSettings("test", "INFO", Duration.ofSeconds(5), Duration.ofSeconds(5),
            "", Duration.ofSeconds(5), "", "h", 6379, "", 0, Duration.ofSeconds(5),
            false, "u", "u", "u", Duration.ofSeconds(5), cron, enabled);
    }
}
