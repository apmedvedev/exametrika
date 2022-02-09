/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.expression;

import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.profiler.probes.AppStackCounterProvider;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IProbeExpressionContext;

/**
 * The {@link ProbeExpressionContext} is a probe expression context.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ProbeExpressionContext implements IProbeExpressionContext {
    private static final ILogger logger = Loggers.get(ProbeExpressionContext.class);
    private final AppStackCounterProvider[] providers = new AppStackCounterProvider[AppStackCounterType.values().length];
    private final IProbeContext context;

    public ProbeExpressionContext(IProbeContext context) {
        Assert.notNull(context);

        this.context = context;
    }

    @Override
    public long getThreadCpuTime() {
        return Times.getThreadCpuTime();
    }

    @Override
    public Object counter(int type) {
        AppStackCounterProvider provider = providers[type];
        if (provider == null)
            provider = createProvider(type);

        try {
            return provider.getValue();
        } catch (Exception e) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);

            return null;
        }
    }

    private synchronized AppStackCounterProvider createProvider(int type) {
        AppStackCounterProvider provider = providers[type];
        if (provider != null)
            return provider;

        provider = new AppStackCounterProvider(AppStackCounterType.values()[type], context);
        providers[type] = provider;
        return provider;
    }
}