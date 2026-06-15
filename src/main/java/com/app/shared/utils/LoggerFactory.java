package com.app.shared.utils;

import org.jboss.logging.Logger;

public final class LoggerFactory {
    private LoggerFactory() {}

    public static Logger create(String name) {
        return Logger.getLogger(name);
    }
}
