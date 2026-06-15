package com.app.jobs;

import java.util.List;

import com.app.core.BaseJob;
import com.app.infrastructure.health.HealthChecker;
import com.app.shared.config.AppSettings;

public final class RegisterJobs {
    private RegisterJobs() {}

    public static List<BaseJob> register(HealthChecker checker, AppSettings settings) {
        return List.of(new HealthCheckJob(checker, settings));
    }
}
