package com.exametrika.impl.boot.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;


/**
 * The {@link Loggers} is simple logger implementation for java agent.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Loggers {
    static final boolean DEBUG = System.getenv("EXA_DEBUG") != null || System.getProperty("com.exametrika.debug", "false").equals("true");

    public static final void logDebug(String logger, String format, Object... arguments) {
        if (!DEBUG)
            return;

        log(logger, "DEBUG", MessageFormat.format(format, arguments), null);
    }

    public static final void logError(String logger, String format, Object... arguments) {
        log(logger, "ERROR", MessageFormat.format(format, arguments), null);
    }

    public static final void logError(String logger, Throwable e) {
        log(logger, "ERROR", null, e);
    }

    private static void log(String logger, String level, String message, Throwable exception) {
        StringBuilder builder = new StringBuilder();
        if (message != null)
            builder.append(message.toString());

        if (exception != null) {
            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            exception.printStackTrace(printWriter);

            if (builder.length() != 0)
                builder.append(" ");

            builder.append(writer.toString());
        }

        String logMessage = MessageFormat.format("{0} [{1}] {2} {3} - {4}", new Date(), Thread.currentThread().getName(),
                level, logger, builder.toString());

        System.out.println(logMessage);
    }

    private Loggers() {
    }
}
