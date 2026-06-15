package com.app;

import com.app.core.CronUtilsAdapter;
import com.app.core.Scheduler;
import com.app.infrastructure.database.DataSourceProvider;
import com.app.infrastructure.health.DefaultHealthChecker;
import com.app.infrastructure.messaging.RabbitMqProvider;
import com.app.infrastructure.redis.RedisProvider;
import com.app.jobs.RegisterJobs;
import com.app.shared.config.AppSettings;
import com.app.shared.utils.LoggerFactory;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
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

        DataSourceProvider dataSource = new DataSourceProvider();
        RedisProvider redis = new RedisProvider();
        RabbitMqProvider rabbit = new RabbitMqProvider();

        try {
            if (settings.messagingEnabled()) {
                rabbit.init(settings.rabbitUrl(), settings.rabbitUser(),
                    settings.rabbitPassword(), settings.rabbitPublishTimeout().toMillis());
                rabbit.connect();
                LOG.info("rabbit connected");
            }
        } catch (Exception e) {
            LOG.errorf(e, "startup failed (rabbit)");
            return 1;
        }

        Scheduler scheduler = new Scheduler(
            RegisterJobs.register(
                new DefaultHealthChecker(dataSource, redis, rabbit, settings),
                settings),
            new CronUtilsAdapter(),
            settings.jobExecutionTimeout(),
            LOG);

        try {
            scheduler.start();
        } catch (Exception e) {
            LOG.errorf(e, "scheduler start failed");
            return 1;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("shutdown requested, draining...");
            scheduler.stop();
            try { rabbit.close(); } catch (Exception ignored) {}
            try { redis.close(); } catch (Exception ignored) {}
            try { dataSource.close(); } catch (Exception ignored) {}
            LOG.info("job-service-java stopped");
        }, "shutdown-hook"));

        return 0;
    }
}
