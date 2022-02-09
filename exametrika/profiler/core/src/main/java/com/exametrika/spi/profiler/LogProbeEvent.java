/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;


/**
 * The {@link LogProbeEvent} is a log probe event.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class LogProbeEvent {
    private final String logger;
    private final String level;
    private final String message;
    private final String thread;
    private final Throwable exception;
    private final long time;

    public LogProbeEvent(String logger, String level, String message, String thread, Throwable exception, long time) {
        if (logger == null)
            logger = "<unknown>";
        if (level == null)
            level = "<unknown>";
        if (thread == null)
            thread = "<unknown>";

        this.logger = logger;
        this.level = level;
        this.message = message;
        this.thread = thread;
        this.exception = exception;
        this.time = time;
    }

    public String getLogger() {
        return logger;
    }

    public String getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getThread() {
        return thread;
    }

    public Throwable getException() {
        return exception;
    }

    public long getTime() {
        return time;
    }
}
