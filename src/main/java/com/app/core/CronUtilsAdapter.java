package com.app.core;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

public class CronUtilsAdapter implements CronAdapter {

    private final CronParser parser;

    public CronUtilsAdapter() {
        CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
        this.parser = new CronParser(definition);
    }

    @Override
    public Optional<CronSchedule> parse(String expression) {
        if (expression == null || expression.isBlank()) {
            return Optional.empty();
        }
        try {
            Cron cron = parser.parse(expression);
            cron.validate();
            return Optional.of(new CronUtilsSchedule(cron));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

class CronUtilsSchedule implements CronSchedule {
    private final ExecutionTime executionTime;

    CronUtilsSchedule(Cron cron) {
        this.executionTime = ExecutionTime.forCron(cron);
    }

    @Override
    public ZonedDateTime next(ZonedDateTime from) {
        ZonedDateTime normalized = from.withZoneSameInstant(ZoneId.of("UTC"));
        return executionTime.nextExecution(normalized)
            .orElseThrow(() -> new IllegalStateException("no next execution"));
    }
}
