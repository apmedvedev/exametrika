/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.probes;

import com.exametrika.api.metrics.exa.config.ExaLogProbeConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.log.impl.LogEvent;
import com.exametrika.common.log.impl.LogInterceptor;
import com.exametrika.impl.profiler.probes.LogProbeCollector;
import com.exametrika.impl.profiler.probes.LogProbeCollector.CollectorInfo;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.ThreadLocalSlot;
import com.exametrika.spi.profiler.ILogExpressionContext;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.AbstractLogProbe;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalSlot;


/**
 * The {@link ExaLogProbe} is a Exa log probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaLogProbe extends AbstractLogProbe {
    public ExaLogProbe(ExaLogProbeConfiguration configuration, IProbeContext context) {
        super(configuration, context, "new @com.exametrika.spi.profiler.LogProbeEvent@(logger, $.log.normalizeLevel(level), " +
                "message, $.exa.currentThread, exception, time)");
    }

    @Override
    public boolean isSystem() {
        return true;
    }

    @Override
    public synchronized void start() {
        LogInterceptor.INSTANCE = new ExaLogInterceptor();
    }

    @Override
    public synchronized void stop() {
        LogInterceptor.INSTANCE = new LogInterceptor();
    }

    @Override
    protected ILogExpressionContext createLogContext() {
        return new ExaContext();
    }

    @Override
    protected Container getContainer() {
        return ((ThreadLocalAccessor) threadLocalAccessor).get(false);
    }

    @Override
    protected CollectorInfo getSlotInfo(IThreadLocalSlot slot) {
        return ((ThreadLocalSlot) slot).get(false);
    }

    @Override
    protected LogProbeCollector createLogCollector(IScope scope, JsonObject metadata, IThreadLocalSlot slot) {
        return new ExaLogProbeCollector(configuration, context, scope, slot, getContainer(), metadata,
                configuration.getComponentType());
    }

    public static class ExaContext implements ILogExpressionContext {
        @Override
        public String normalizeLevel(String level) {
            if (level == null)
                return "";

            return level.toLowerCase();
        }
    }

    private class ExaLogInterceptor extends LogInterceptor {
        @Override
        public void onLog(LogEvent event) {
            onEnter(0, 0, null, new Object[]{event});
        }
    }
}
