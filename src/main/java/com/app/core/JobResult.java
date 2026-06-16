package com.app.core;

public record JobResult(String job, JobStatus status, long durationMs, String error) {
}
