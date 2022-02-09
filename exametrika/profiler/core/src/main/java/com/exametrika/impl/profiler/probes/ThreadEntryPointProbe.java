/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.profiler.config.ThreadEntryPointProbeConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.profiler.boot.ThreadEntryPointProbeInterceptor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.Request;


/**
 * The {@link ThreadEntryPointProbe} is a thread entry point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ThreadEntryPointProbe extends EntryPointProbe {
    public ThreadEntryPointProbe(ThreadEntryPointProbeConfiguration configuration, IProbeContext context, int index) {
        super(configuration, context, index);
    }

    @Override
    public synchronized void start() {
        ThreadEntryPointProbeInterceptor.interceptor = this;
    }

    @Override
    public synchronized void stop() {
        ThreadEntryPointProbeInterceptor.interceptor = null;
    }

    @Override
    protected EntryPointProbeCollector doCreateCollector(int index, String combineId, ICallPath callPath,
                                                         String name, StackProbeRootCollector root, StackProbeCollector parent, JsonObject metadata, boolean primary, boolean leaf) {
        return new ThreadEntryPointProbeCollector(index, this, name, combineId,
                callPath, root, parent, metadata, primary, leaf);
    }

    @Override
    public Object onEnter(int index, int version, Object instance, Object[] params) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall || isRecursive())
            return null;

        container.inCall = true;
        container.scopes.deactivateAll();

        ThreadRequest request = (ThreadRequest) params[0];
        beginRequest(container, request, request.getTag());
        request.setStartTime(context.getTimeSource().getCurrentTime());

        setRecursive(true);
        container.inCall = false;

        return request;
    }

    @Override
    public void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return;

        container.inCall = true;
        setRecursive(false);

        ThreadRequest request = (ThreadRequest) param;
        request.setEndTime(context.getTimeSource().getCurrentTime());
        endRequest(container, null);

        container.scopes.activateAll();
        container.inCall = false;
    }

    @Override
    public void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        onReturnExit(index, version, param, instance, null);
    }

    @Override
    protected IRequest mapScope(Object rawRequest) {
        return new Request("thread", null);
    }
}
