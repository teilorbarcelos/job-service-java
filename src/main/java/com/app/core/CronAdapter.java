package com.app.core;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface CronAdapter {
    Optional<CronSchedule> parse(String expression);
}

interface CronSchedule {
    ZonedDateTime next(ZonedDateTime from);
}
