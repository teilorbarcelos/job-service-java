package com.app;

import javax.sql.DataSource;

import com.app.core.CronUtilsAdapter;
import com.app.core.Scheduler;
import com.app.infrastructure.database.DataSourceProvider;
import com.app.infrastructure.health.DefaultHealthChecker;
import com.app.infrastructure.health.HealthChecker;
import com.app.infrastructure.messaging.RabbitMqProvider;
import com.app.infrastructure.redis.RedisProvider;
import com.app.jobs.RegisterJobs;
import com.app.shared.config.AppSettings;
import com.app.shared.utils.LoggerFactory;
import com.app.core.BaseJob;
import com.app.core.CronAdapter;

import io.quarkus.arc.DefaultBean;
import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class App {

    private static final Logger LOG = LoggerFactory.create("com.app");

    private final Scheduler scheduler;
    private final DataSourceProvider dataSource;
    private final RedisProvider redis;
    private final RabbitMqProvider rabbit;
    private final AppSettings settings;

    public App(Scheduler scheduler, DataSourceProvider dataSource, RedisProvider redis,
              RabbitMqProvider rabbit, AppSettings settings) {
        this.scheduler = scheduler;
        this.dataSource = dataSource;
        this.redis = redis;
        this.rabbit = rabbit;
        this.settings = settings;
    }

    public Scheduler scheduler() { return scheduler; }
    public DataSourceProvider dataSource() { return dataSource; }
    public RedisProvider redis() { return redis; }
    public RabbitMqProvider rabbit() { return rabbit; }
    public AppSettings settings() { return settings; }

    public void shutdown() {
        scheduler.stop();
        try { rabbit.close(); } catch (Exception ignored) {}
        try { redis.close(); } catch (Exception ignored) {}
        try { dataSource.close(); } catch (Exception ignored) {}
    }

    @Produces
    @Singleton
    public AppSettings appSettings() {
        return AppSettings.load();
    }

    @Produces
    @ApplicationScoped
    public DataSourceProvider dataSourceProvider(javax.sql.DataSource ds) {
        return new DataSourceProvider(ds);
    }

    @Produces
    @ApplicationScoped
    public RedisProvider redisProvider(RedisDataSource rds) {
        return new RedisProvider(rds);
    }

    @Produces
    @ApplicationScoped
    public RabbitMqProvider rabbitProvider(AppSettings settings) throws Exception {
        var p = new RabbitMqProvider();
        if (settings.messagingEnabled()) {
            p.init(settings.rabbitUrl(), settings.rabbitUser(),
                settings.rabbitPassword(), settings.rabbitPublishTimeout().toMillis());
            p.connect();
            LOG.info("rabbit connected");
        }
        return p;
    }

    @Produces
    @ApplicationScoped
    public HealthChecker healthChecker(DataSourceProvider ds, RedisProvider r,
                                       RabbitMqProvider rb, AppSettings s) {
        return new DefaultHealthChecker(ds, r, rb, s);
    }

    @Produces
    @ApplicationScoped
    public CronAdapter cronAdapter() {
        return new CronUtilsAdapter();
    }

    @Produces
    @ApplicationScoped
    public Scheduler scheduler(HealthChecker checker, CronAdapter cron, AppSettings s) {
        List<BaseJob> jobs = RegisterJobs.register(checker, s);
        return new Scheduler(jobs, cron, s.jobExecutionTimeout(), LOG);
    }

    public static App bootstrap(AppSettings settings) {
        try {
            // Manual bootstrap path: used in tests and ad-hoc runs
            // where CDI isn't available. Production uses the @Produces
            // methods above via Quarkus DI.
            throw new UnsupportedOperationException(
                "Use the Quarkus runtime: it will instantiate App via @Inject");
        } catch (Exception e) {
            return null;
        }
    }
}
