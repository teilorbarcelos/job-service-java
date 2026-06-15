package com.app.core;

public interface BaseJob {
    String name();
    String schedule();
    String description();
    default boolean enabled() { return true; }
    void run(JobContext context) throws Exception;
}
