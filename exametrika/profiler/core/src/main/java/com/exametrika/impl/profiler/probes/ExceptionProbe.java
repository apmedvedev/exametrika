/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.api.profiler.config.ExceptionProbeConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.profiler.boot.ExceptionProbeInterceptor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.profiler.IProbeCollector;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalProvider;
import com.exametrika.spi.profiler.IThreadLocalSlot;


/**
 * The {@link ExceptionProbe} is an exception probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExceptionProbe extends BaseProbe implements IThreadLocalProvider {
    private final ExceptionProbeConfiguration configuration;
    private IThreadLocalSlot slot;

    public ExceptionProbe(ExceptionProbeConfiguration configuration, IProbeContext context) {
        super(configuration, context);

        this.configuration = configuration;
    }

    @Override
    public synchronized void start() {
        ExceptionProbeInterceptor.interceptor = this;
    }

    @Override
    public synchronized void stop() {
        ExceptionProbeInterceptor.interceptor = null;
    }

    @Override
    public boolean isStack() {
        return false;
    }

    @Override
    public IProbeCollector createCollector(IScope scope) {
        Assert.isTrue(scope.isPermanent() || scope.getEntryPointComponentType() != null);
        JsonObject metadata = Json.object()
                .put("node", context.getConfiguration().getNodeName())
                .put("type", "exception,jvm," + (scope.isPermanent() ? "background" : "transaction"))
                .putIf("entry", scope.getEntryPointComponentType(), scope.getEntryPointComponentType() != null)
                .toObject();
        return new ExceptionProbeCollector(configuration, context, scope, slot, threadLocalAccessor.get(), metadata,
                configuration.getComponentType());
    }

    @Override
    public void onTimer() {
    }

    @Override
    public void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return;

        container.inCall = true;

        CollectorInfo info = slot.get();
        if (info.collector != null && instance instanceof Throwable && (instance.getClass().getClassLoader() != getClass().getClassLoader())) {
            Throwable exception = (Throwable) instance;
            info.collector.measure(exception);
        }

        container.inCall = false;

        return;
    }

    @Override
    public void setSlot(IThreadLocalSlot slot) {
        this.slot = slot;
    }

    @Override
    public Object allocate() {
        return new CollectorInfo();
    }

    static class CollectorInfo {
        public ExceptionProbeCollector collector;
    }
}
