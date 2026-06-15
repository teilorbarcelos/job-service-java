# job-service-java

> Scheduled job runner skeleton for Java 21 / Quarkus 3. Connects to
> `backend-java-quarkus` to consume PostgreSQL, Redis, and RabbitMQ.

A clean, idiomatic boilerplate for running cron-scheduled jobs in Java —
no HTTP layer, no auth, no audit, no PDF, no storage. Just jobs.

## Stack

- **Java 21**
- **Quarkus 3.21** (Worker mode — no HTTP)
- **PostgreSQL** via `agroal` + `quarkus-jdbc-postgresql` (no Hibernate, no Flyway)
- **Redis** via `quarkus-redis-client`
- **RabbitMQ** via `com.rabbitmq:amqp-client` (via `quarkus-messaging-rabbitmq`)
- **Cron** via `com.cronutils:cron-utils`
- **Logging** via `org.jboss.logging.Logger`
- **CDI** via `quarkus-arc`
- **Tests** via JUnit 5 + Mockito + JaCoCo

## Architecture

```
src/main/java/com/app/
├── Application.java                    # @QuarkusMain entry point
├── core/
│   ├── BaseJob.java                    # Interface (name, schedule, run)
│   ├── JobContext.java                 # Logger passed to Run
│   ├── JobResult.java                  # Status, DurationMs, Error
│   ├── JobStatus.java                  # SUCCESS, FAILED, CANCELLED, TIMEOUT
│   ├── JobExecutor.java                # Stopwatch + CompletableFuture timeout
│   ├── CronAdapter.java                # Testable seam (cron-utils impl)
│   ├── CronUtilsAdapter.java           # Real impl
│   └── Scheduler.java                  # ScheduledExecutorService + cron loop
├── infrastructure/
│   ├── database/DataSourceProvider.java  # Agroal DataSource wrapper
│   ├── redis/RedisProvider.java          # Quarkus RedisDataSource wrapper
│   ├── messaging/RabbitMqProvider.java   # RabbitMQ.Client publisher + check
│   └── health/                          # DefaultHealthChecker (PG/Redis/Rabbit)
├── jobs/
│   ├── HealthCheckJob.java             # Example: status a cada minuto
│   └── RegisterJobs.java               # Central registration
└── shared/
    ├── config/AppSettings.java         # Record + env loaders
    ├── errors/AppError.java            # Hierarchy
    └── utils/                          # LoggerFactory, AwaitShutdown
```

## Quick start

### 1. Subir infra local

```bash
make infra-up
```

### 2. Configurar `.env`

```bash
cp .env.example .env
# editar DATABASE_URL, RABBIT_URL, etc.
```

### 3. Rodar em dev

```bash
make dev
```

### 4. Adicionar um job

```java
// src/main/java/com/app/jobs/CleanupJob.java
package com.app.jobs;

import com.app.core.BaseJob;
import com.app.core.JobContext;
import com.app.shared.config.AppSettings;

public class CleanupJob implements BaseJob {
    @Override public String name() { return "cleanup"; }
    @Override public String schedule() { return "0 3 * * *"; }
    @Override public String description() { return "Remove registros antigos"; }
    @Override public void run(JobContext context) {
        context.logger().info("running cleanup");
        // ... sua lógica
    }
}
```

```java
// src/main/java/com/app/jobs/RegisterJobs.java
public static List<BaseJob> register(HealthChecker checker, AppSettings settings) {
    return List.of(
        new HealthCheckJob(checker, settings),
        new CleanupJob(),  // ← novo
    );
}
```

## Comandos

```bash
make dev          # Quarkus dev (hot reload)
make test         # mvn test
make coverage     # mvn verify (Jacoco)
make lint         # mvn formatter:validate
make check        # lint + test
make build        # mvn package (fast-jar)
make docker       # build image
make run          # java -jar target/quarkus-app/quarkus-run.jar
make infra-up     # docker compose up (PG+Redis+Rabbit)
make infra-down   # docker compose down
make sonar        # SonarQube scan
make clean
```

## Configuração (env vars)

| Var | Default | Descrição |
|---|---|---|
| `ENVIRONMENT` | `local` | dev / staging / production |
| `LOG_LEVEL` | `INFO` | TRACE/DEBUG/INFO/WARN/ERROR/FATAL |
| `SHUTDOWN_TIMEOUT_SECONDS` | `30` | Max wait for cleanup on SIGTERM |
| `JOB_EXECUTION_TIMEOUT_SECONDS` | `300` | Per-job timeout |
| `DATABASE_URL` | (required) | PostgreSQL JDBC URL |
| `DATABASE_COMMAND_TIMEOUT_SECONDS` | `10` | SELECT timeout |
| `REDIS_URL` | (empty) | URL (preferred) |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | (if no URL) |
| `REDIS_PASSWORD` | (empty) | |
| `REDIS_DB` | `0` | |
| `MESSAGING_ENABLED` | `false` | Enable RabbitMQ publisher |
| `RABBIT_URL` | `amqp://guest:guest@localhost:5672/` | |
| `RABBIT_USER` / `RABBIT_PASSWORD` | `guest` | |
| `RABBITMQ_PUBLISH_TIMEOUT` | `5` | seconds |
| `HEALTH_CHECK_CRON` | `*/1 * * * *` | 5-field cron |
| `HEALTH_CHECK_ENABLED` | `true` | Disable health check |

## Princípios

- **S** Single Responsibility — cada job tem um único propósito
- **O** Open/Closed — adicionar um job = criar uma classe + 1 linha de registro
- **L** Liskov Substitution — todo `BaseJob` é intercambiável
- **I** Interface Segregation — dependências injetadas via construtor
- **D** Dependency Inversion — jobs dependem de abstrações, não de implementações
- **DRY** — lógica compartilhada fica em `JobExecutor`
- **Clean Code** — nomes expressivos, funções curtas, sem side-effects

## Testes

```bash
make test
```

Coverage via JaCoCo (excluindo `Application.java`):
- `core` — 100%
- `infrastructure/database` — 100%
- `infrastructure/health` — 100%
- `infrastructure/messaging` — 100%
- `infrastructure/redis` — 100%
- `jobs` — 100%
- `shared` — 100%

## CI

GitHub Actions roda em push/PR para `develop` e `main`:
- `mvn formatter:validate` (lint)
- `mvn test` com JaCoCo
- Coverage gate: ≥85% line (excluindo `Application.java`)

## License

This is an open-source boilerplate. Use freely.
