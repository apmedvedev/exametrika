/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.profiler.config.AllocationProbeConfiguration;
import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.impl.profiler.boot.AllocationProbeInterceptor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.profiler.IProbeCollector;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalProvider;
import com.exametrika.spi.profiler.IThreadLocalSlot;


/**
 * The {@link AllocationProbe} is an allocation probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AllocationProbe extends BaseProbe implements IThreadLocalProvider {
    private IThreadLocalSlot slot;

    public AllocationProbe(AllocationProbeConfiguration configuration, IProbeContext context) {
        super(configuration, context);
    }

    @Override
    public synchronized void start() {
        AllocationProbeInterceptor.interceptor = this;
    }

    @Override
    public synchronized void stop() {
        AllocationProbeInterceptor.interceptor = null;
    }

    @Override
    public boolean isStack() {
        return true;
    }

    @Override
    public IProbeCollector createCollector(IScope scope) {
        return null;
    }

    @Override
    public void onTimer() {
    }

    @Override
    public Object onEnter(int index, int version, Object instance, Object[] params) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return null;

        CollectorInfo info = slot.get();
        info.recursionCount++;
        return this;
    }

    @Override
    public void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return;

        CollectorInfo info = slot.get();
        info.recursionCount--;

        IJoinPoint joinPoint = context.getJoinPointProvider().findJoinPoint(index, version);
        if (joinPoint == null)
            return;

        if (joinPoint.getMethodName().startsWith("newInstance"))
            container.counters[AppStackCounterType.ALLOCATION_COUNT.ordinal()]++;
        else if (info.recursionCount == 0)
            container.counters[AppStackCounterType.CLASSES_COUNT.ordinal()]++;
    }

    @Override
    public void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return;

        CollectorInfo info = slot.get();
        info.recursionCount--;
    }

    @Override
    public void onNewObject(int index, int version, Object instance, Object object) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return;

        container.counters[AppStackCounterType.ALLOCATION_COUNT.ordinal()]++;
    }

    @Override
    public void onNewArray(int index, int version, Object instance, Object array) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return;

        container.counters[AppStackCounterType.ALLOCATION_COUNT.ordinal()]++;
    }

    @Override
    public void setSlot(IThreadLocalSlot slot) {
        this.slot = slot;
    }

    @Override
    public Object allocate() {
        return new CollectorInfo();
    }

    private static class CollectorInfo {
        private int recursionCount;
    }
}
