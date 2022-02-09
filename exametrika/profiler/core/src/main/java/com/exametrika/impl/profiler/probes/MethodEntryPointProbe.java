/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.profiler.config.MethodEntryPointProbeConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.profiler.boot.MethodEntryPointProbeInterceptor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.Request;


/**
 * The {@link MethodEntryPointProbe} is a method entry point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MethodEntryPointProbe extends EntryPointProbe {
    public MethodEntryPointProbe(MethodEntryPointProbeConfiguration configuration, IProbeContext context, int index) {
        super(configuration, context, index);
    }

    @Override
    public synchronized void start() {
        MethodEntryPointProbeInterceptor.interceptor = this;
    }

    @Override
    public synchronized void stop() {
        MethodEntryPointProbeInterceptor.interceptor = null;
    }

    @Override
    protected EntryPointProbeCollector doCreateCollector(int index, String combineId, ICallPath callPath,
                                                         String name, StackProbeRootCollector root, StackProbeCollector parent, JsonObject metadata, boolean primary, boolean leaf) {
        return new MethodEntryPointProbeCollector(index, this, name, combineId,
                callPath, root, parent, metadata, primary, leaf);
    }

    @Override
    public Object onEnter(int index, int version, Object instance, Object[] params) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall || isRecursive())
            return null;

        IJoinPoint joinPoint = context.getJoinPointProvider().findJoinPoint(index, version);
        if (joinPoint == null)
            return null;

        container.inCall = true;
        container.scopes.deactivateAll();

        Request request = new Request("method:" + joinPoint.getClassName() + "." + joinPoint.getMethodSignature(), null);
        beginRequest(container, request, null);

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
        return new Request("method", null);
    }

    @Override
    protected IRequest mapRequest(IScope scope, Object rawRequest) {
        return (IRequest) rawRequest;
    }
}
