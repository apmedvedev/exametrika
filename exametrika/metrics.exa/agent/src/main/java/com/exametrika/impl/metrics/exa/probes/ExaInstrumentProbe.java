/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.probes;

import com.exametrika.api.metrics.exa.config.ExaInstrumentProbeConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.instrument.InstrumentInterceptor;
import com.exametrika.impl.profiler.probes.BaseProbe;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.ThreadLocalSlot;
import com.exametrika.spi.profiler.IProbeCollector;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalProvider;
import com.exametrika.spi.profiler.IThreadLocalSlot;


/**
 * The {@link ExaInstrumentProbe} is a Exa instrument probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaInstrumentProbe extends BaseProbe implements IThreadLocalProvider {
    private final ExaInstrumentProbeConfiguration configuration;
    private ThreadLocalSlot slot;

    public ExaInstrumentProbe(ExaInstrumentProbeConfiguration configuration, IProbeContext context) {
        super(configuration, context);

        this.configuration = configuration;
    }

    @Override
    public boolean isSystem() {
        return true;
    }

    @Override
    public synchronized void start() {
        InstrumentInterceptor.INSTANCE = new ExaInstrumentInterceptor();
    }

    @Override
    public synchronized void stop() {
        InstrumentInterceptor.INSTANCE = new InstrumentInterceptor();
    }

    @Override
    public boolean isStack() {
        return false;
    }

    @Override
    public IProbeCollector createCollector(IScope scope) {
        JsonObject metadata = Json.object()
                .put("node", context.getConfiguration().getNodeName())
                .put("type", "exa," + (scope.isPermanent() ? "background" : "transaction"))
                .toObject();
        return new ExaInstrumentProbeCollector(configuration, context, scope, slot, threadLocalAccessor.get(false), metadata);
    }

    @Override
    public void onTimer() {
    }

    @Override
    public void setSlot(IThreadLocalSlot slot) {
        this.slot = (ThreadLocalSlot) slot;
    }

    @Override
    public Object allocate() {
        return new CollectorInfo();
    }

    static class CollectorInfo {
        public ExaInstrumentProbeCollector collector;
    }

    private class ExaInstrumentInterceptor extends InstrumentInterceptor {
        @Override
        public boolean onBeforeTransform() {
            Container container = threadLocalAccessor.get(false);
            if (container == null)
                return false;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return false;

            info.collector.beginMeasure();
            return true;
        }

        @Override
        public void onTransformSuccess(int beforeClassSize, int afterClassSize, int joinPointCount) {
            Container container = threadLocalAccessor.get(false);
            if (container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.endMeasureSuccess(beforeClassSize, afterClassSize, joinPointCount);
        }

        @Override
        public void onTransformError(String className, Throwable exception) {
            Container container = threadLocalAccessor.get(false);
            if (container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.endMeasureError(className, exception);
        }

        @Override
        public void onTransformSkip() {
            Container container = threadLocalAccessor.get(false);
            if (container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.endMeasureSkip();
        }

        @Override
        public void onAfterTransform() {
            Container container = threadLocalAccessor.get(false);
            if (container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.endMeasure();
        }
    }
}
