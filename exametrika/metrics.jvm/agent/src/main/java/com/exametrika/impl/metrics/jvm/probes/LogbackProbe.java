/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import com.exametrika.impl.metrics.jvm.boot.LogbackProbeInterceptor;
import com.exametrika.spi.profiler.ILogExpressionContext;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.AbstractLogProbe;
import com.exametrika.spi.profiler.config.LogProbeConfiguration;


/**
 * The {@link LogbackProbe} is a Logback log probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class LogbackProbe extends AbstractLogProbe {
    public LogbackProbe(LogProbeConfiguration configuration, IProbeContext context) {
        super(configuration, context, "new @com.exametrika.spi.profiler.LogProbeEvent@(loggerName, $.log.normalizeLevel(level), " +
                "formattedMessage, $.exa.currentThread, $context?.throwableProxy?.throwable, timeStamp)");
    }

    @Override
    public synchronized void start() {
        LogbackProbeInterceptor.interceptor = this;
    }

    @Override
    public synchronized void stop() {
        LogbackProbeInterceptor.interceptor = null;
    }

    @Override
    protected ILogExpressionContext createLogContext() {
        return new LogbackContext();
    }

    public static class LogbackContext implements ILogExpressionContext {
        @Override
        public String normalizeLevel(String level) {
            if (level == null)
                return "";

            if (level.equals("ERROR"))
                return "error";
            else if (level.equals("WARN"))
                return "warning";
            else if (level.equals("INFO"))
                return "info";
            else if (level.equals("DEBUG"))
                return "debug";
            else if (level.equals("TRACE"))
                return "trace";
            else
                return level.toLowerCase();
        }
    }
}
