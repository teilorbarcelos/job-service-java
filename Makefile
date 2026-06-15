.PHONY: help dev test coverage lint check build docker run infra-up infra-down sonar clean

help:
	@echo "Job Service Java — available targets:"
	@echo "  make dev          Run in dev mode (Quarkus dev)"
	@echo "  make test         Run unit tests"
	@echo "  make coverage     Run tests with coverage"
	@echo "  make lint         Run formatter check (no-op in Java)"
	@echo "  make check        Run lint + test + coverage gate"
	@echo "  make build        Build the JAR (fast-jar)"
	@echo "  make docker       Build Docker image"
	@echo "  make run          Run the application"
	@echo "  make infra-up     Start PG + Redis + Rabbit via docker compose"
	@echo "  make infra-down   Stop the dev infrastructure"
	@echo "  make sonar        Run SonarQube scan"
	@echo "  make clean        Remove build artifacts"

dev:
	./mvnw quarkus:dev

test:
	./mvnw -B -ntp test

coverage:
	./mvnw -B -ntp verify -DskipITs
	@echo ""
	@echo "Coverage report: target/site/jacoco/index.html"

lint:
	./mvnw -B -ntp formatter:validate

check: lint test
	@echo "✅ All checks passed"

build:
	./mvnw -B -ntp -DskipTests package

docker:
	docker build -t job-service-java:latest .

run:
	java -jar target/quarkus-app/quarkus-run.jar

infra-up:
	docker compose -f docker-compose.infra.yml up -d

infra-down:
	docker compose -f docker-compose.infra.yml down

sonar:
	./scripts/sonar-scan.sh "job-service-java" "job-service-java"

clean:
	./mvnw -B -ntp clean
	rm -rf target/
