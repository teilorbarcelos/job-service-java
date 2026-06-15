package com.app;

import com.app.core.CronUtilsAdapter;
import com.app.core.Scheduler;
import com.app.jobs.RegisterJobs;
import com.app.shared.config.AppSettings;
import com.app.shared.utils.LoggerFactory;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.jboss.logging.Logger;

@QuarkusMain
public class Application implements QuarkusApplication {

    private static final Logger LOG = LoggerFactory.create("com.app", "INFO");

    public static void main(String[] args) {
        Quarkus.run(Application.class, args);
    }

    @Override
    public int run(String... args) {
        AppSettings settings = AppSettings.load();
        LOG.infof("Starting job-service-java (env=%s, log=%s, execTimeout=%ds)",
            settings.environment(), settings.logLevel(),
            settings.jobExecutionTimeout().toSeconds());

        App app = App.bootstrap(settings);
        if (app == null) return 1;

        try {
            app.scheduler().start();
        } catch (Exception e) {
            LOG.errorf(e, "scheduler start failed");
            return 1;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("shutdown requested, draining...");
            app.shutdown();
            LOG.info("job-service-java stopped");
        }, "shutdown-hook"));

        return 0;
    }
}
