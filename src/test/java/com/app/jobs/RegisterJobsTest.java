package com.app.jobs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.app.core.BaseJob;
import com.app.infrastructure.health.HealthChecker;
import com.app.shared.config.AppSettings;

class RegisterJobsTest {

    @Test
    void register_returns_health_check_job_enabled() {
        var checker = mock(HealthChecker.class);
        var settings = settings(true);
        List<BaseJob> jobs = RegisterJobs.register(checker, settings);
        assertEquals(1, jobs.size());
        assertEquals("health-check", jobs.get(0).name());
        assertTrue(jobs.get(0).enabled());
    }

    @Test
    void register_disables_job_when_setting_false() {
        var checker = mock(HealthChecker.class);
        var settings = settings(false);
        List<BaseJob> jobs = RegisterJobs.register(checker, settings);
        assertFalse(jobs.get(0).enabled());
    }

    private static AppSettings settings(boolean healthCheckEnabled) {
        return new AppSettings("test", "INFO", Duration.ofSeconds(5), Duration.ofSeconds(5), "", Duration.ofSeconds(5),
                "", "h", 6379, "", 0, Duration.ofSeconds(5), false, "u", "u", "u", Duration.ofSeconds(5), "*/1 * * * *",
                healthCheckEnabled);
    }
}
