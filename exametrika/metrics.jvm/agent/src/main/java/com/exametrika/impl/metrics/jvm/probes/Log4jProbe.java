/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import com.exametrika.impl.metrics.jvm.boot.Log4jProbeInterceptor;
import com.exametrika.spi.profiler.ILogExpressionContext;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.AbstractLogProbe;
import com.exametrika.spi.profiler.config.LogProbeConfiguration;


/**
 * The {@link Log4jProbe} is a Log4j log probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Log4jProbe extends AbstractLogProbe {
    public Log4jProbe(LogProbeConfiguration configuration, IProbeContext context) {
        super(configuration, context, "new @com.exametrika.spi.profiler.LogProbeEvent@(loggerName, $.log.normalizeLevel(level), " +
                "message, $.exa.currentThread, $context?.throwableInformation?.throwable, timeStamp)");
    }

    @Override
    public synchronized void start() {
        Log4jProbeInterceptor.interceptor = this;
    }

    @Override
    public synchronized void stop() {
        Log4jProbeInterceptor.interceptor = null;
    }

    @Override
    protected ILogExpressionContext createLogContext() {
        return new Log4jContext();
    }

    public static class Log4jContext implements ILogExpressionContext {
        @Override
        public String normalizeLevel(String level) {
            if (level == null)
                return "";

            if (level.equals("ERROR") || level.equals("FATAL"))
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
