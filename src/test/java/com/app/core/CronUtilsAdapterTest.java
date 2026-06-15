package com.app.core;

import static org.junit.jupiter.api.Assertions.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class CronUtilsAdapterTest {

    @Test
    void parse_valid_expression() {
        var adapter = new CronUtilsAdapter();
        Optional<CronSchedule> sched = adapter.parse("*/1 * * * *");
        assertTrue(sched.isPresent());
        ZonedDateTime now = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime next = sched.get().next(now);
        assertEquals(1, next.getMinute());
    }

    @Test
    void parse_invalid_expression_returns_empty() {
        var adapter = new CronUtilsAdapter();
        assertTrue(adapter.parse("not a cron").isEmpty());
    }

    @Test
    void parse_null_or_blank_returns_empty() {
        var adapter = new CronUtilsAdapter();
        assertTrue(adapter.parse(null).isEmpty());
        assertTrue(adapter.parse("").isEmpty());
        assertTrue(adapter.parse("   ").isEmpty());
    }

    @Test
    void next_advances_correctly() {
        var adapter = new CronUtilsAdapter();
        var sched = adapter.parse("0 9 * * *").orElseThrow();
        ZonedDateTime from = ZonedDateTime.of(2026, 6, 15, 8, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime next = sched.next(from);
        assertEquals(9, next.getHour());
        assertEquals(0, next.getMinute());
        assertTrue(next.isAfter(from));
    }
}
