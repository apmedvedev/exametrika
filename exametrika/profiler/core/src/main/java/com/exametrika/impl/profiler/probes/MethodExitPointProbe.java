/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.profiler.config.MethodExitPointProbeConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.profiler.boot.MethodExitPointProbeInterceptor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.Request;


/**
 * The {@link MethodExitPointProbe} is a method exit point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MethodExitPointProbe extends ExitPointProbe {
    public MethodExitPointProbe(MethodExitPointProbeConfiguration configuration, IProbeContext context, int index) {
        super(configuration, context, index, "methodExitPointProbe");
    }

    @Override
    public synchronized void start() {
        MethodExitPointProbeInterceptor.interceptor = this;
    }

    @Override
    public synchronized void stop() {
        MethodExitPointProbeInterceptor.interceptor = null;
    }

    @Override
    public Object onEnter(int index, int version, Object instance, Object[] params) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall || container.top == null || isRecursive())
            return null;

        IJoinPoint joinPoint = context.getJoinPointProvider().findJoinPoint(index, version);
        if (joinPoint == null)
            return null;

        container.inCall = true;

        Request request = new Request(joinPoint.getClassName() + "." + joinPoint.getMethodSignature(), null);
        beginRequest(container, request);

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
        return new MethodExitPointProbeCollector((MethodExitPointProbeConfiguration) configuration, index, name, stackId,
                callPath, root, parent, metadata, calibrateInfo, leaf);
    }

    @Override
    protected IRequest mapRequest(IScope scope, Object rawRequest) {
        return (IRequest) rawRequest;
    }

    @Override
    protected Object createCalibratingRequest() {
        return new Request("calibrate", null);
    }
}
