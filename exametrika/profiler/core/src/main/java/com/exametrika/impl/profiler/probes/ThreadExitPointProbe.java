/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.api.profiler.config.ThreadExitPointProbeConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.profiler.boot.IThreadExitPointInterceptor;
import com.exametrika.impl.profiler.boot.ThreadExitPointProbeInterceptor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.TraceTag;


/**
 * The {@link ThreadExitPointProbe} is a thread exit point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ThreadExitPointProbe extends ExitPointProbe implements IThreadExitPointInterceptor {
    public ThreadExitPointProbe(ThreadExitPointProbeConfiguration configuration, IProbeContext context, int index) {
        super(configuration, context, index, "threadExitPointProbe");
    }

    @Override
    public synchronized void start() {
        ThreadExitPointProbeInterceptor.interceptor = this;
    }

    @Override
    public synchronized void stop() {
        ThreadExitPointProbeInterceptor.interceptor = null;
    }

    @Override
    public Runnable onExecute(int index, int version, Object task) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall || container.top == null || isRecursive())
            return (Runnable) task;

        container.inCall = true;

        ThreadRequest request = new ThreadRequest((Runnable) task);

        container.inCall = false;

        return request;
    }

    @Override
    public Object onEnter(int index, int version, Object instance, Object[] params) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall || container.top == null || isRecursive())
            return null;

        container.inCall = true;

        Object request = createRequest(container, instance, params);
        if (request != null)
            beginRequest(container, request);
        else
            request = this;

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

        if (param instanceof ThreadRequest)
            endRequest(container, null, 0, 0);

        container.inCall = false;
    }

    @Override
    public void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        onReturnExit(index, version, param, instance, null);
    }

    @Override
    protected ExitPointProbeCollector doCreateCollector(int index, String name, UUID stackId, ICallPath callPath,
                                                        StackProbeRootCollector root, StackProbeCollector parent, JsonObject metadata, ExitPointProbeCalibrateInfo calibrateInfo,
                                                        boolean leaf) {
        return new ThreadExitPointProbeCollector((ThreadExitPointProbeConfiguration) configuration, index, name, stackId,
                callPath, root, parent, metadata, calibrateInfo, leaf);
    }

    @Override
    protected IRequest mapRequest(IScope scope, Object rawRequest) {
        return (IRequest) rawRequest;
    }

    @Override
    protected void writeTag(Object request, TraceTag tag) {
        ThreadRequest threadRequest = (ThreadRequest) request;
        threadRequest.setTag(tag);
    }

    @Override
    protected Object createCalibratingRequest() {
        return new ThreadRequest(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    private ThreadRequest createRequest(Container container, Object instance, Object[] params) {
        ThreadRequest request = null;
        if (instance instanceof Thread) {
            Thread thread = (Thread) instance;
            container.counters[AppStackCounterType.THREADS_COUNT.ordinal()]++;

            if (!thread.isDaemon()) {
                Runnable target = ThreadLocalAccessor.getThreadTarget(thread);
                request = new ThreadRequest(target);
                ThreadLocalAccessor.setThreadTarget(thread, request);
            }
        } else if (params[0] instanceof ThreadRequest)
            request = (ThreadRequest) params[0];

        return request;
    }
}
