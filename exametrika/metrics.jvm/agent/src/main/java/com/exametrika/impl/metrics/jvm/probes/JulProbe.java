/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import com.exametrika.impl.metrics.jvm.boot.JulProbeInterceptor;
import com.exametrika.spi.profiler.ILogExpressionContext;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.AbstractLogProbe;
import com.exametrika.spi.profiler.config.LogProbeConfiguration;


/**
 * The {@link JulProbe} is a java.util.logging log probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JulProbe extends AbstractLogProbe {
    public JulProbe(LogProbeConfiguration configuration, IProbeContext context) {
        super(configuration, context, "new @com.exametrika.spi.profiler.LogProbeEvent@(loggerName, $.log.normalizeLevel(level), " +
                "message, $.exa.currentThread, thrown, millis)");
    }

    @Override
    public synchronized void start() {
        JulProbeInterceptor.interceptor = this;
    }

    @Override
    public synchronized void stop() {
        JulProbeInterceptor.interceptor = null;
    }

    @Override
    protected ILogExpressionContext createLogContext() {
        return new JulContext();
    }

    public static class JulContext implements ILogExpressionContext {
        @Override
        public String normalizeLevel(String level) {
            if (level == null)
                return "";

            if (level.equals("SEVERE"))
                return "error";
            else if (level.equals("WARNING"))
                return "warning";
            else if (level.equals("INFO"))
                return "info";
            else if (level.equals("FINE"))
                return "debug";
            else if (level.equals("FINER") || level.equals("FINEST"))
                return "trace";
            else
                return level.toLowerCase();
        }
    }
}
