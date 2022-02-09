/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.profiler.probes.DelegatingExitPointProbe;
import com.exametrika.spi.profiler.boot.ThreadLocalContainer;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;


/**
 * The {@link AbstractExitPointProbe} is an exit point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AbstractExitPointProbe extends AbstractProbe {
    private DelegatingExitPointProbe baseProbe;

    public AbstractExitPointProbe(ExitPointProbeConfiguration configuration, IProbeContext context) {
        super(configuration, context);
    }

    public final IProbeContext getContext() {
        return context;
    }

    public final void setBaseProbe(IProbe probe) {
        Assert.notNull(probe);

        this.baseProbe = (DelegatingExitPointProbe) probe;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    public abstract AbstractExitPointProbeCollector createCollector();

    public abstract IRequest mapScope(Object rawRequest);

    public IRequest mapRequest(IScope scope, Object rawRequest) {
        return baseProbe.superMapRequest(scope, rawRequest);
    }

    public void writeTag(Object request, TraceTag tag) {
        Assert.supports(false);
    }

    public abstract Object createCalibratingRequest();

    protected final void beginRequest(ThreadLocalContainer container, Object rawRequest) {
        baseProbe.beginRequest(container, rawRequest);
    }

    protected final void endRequest(ThreadLocalContainer container, JsonObject error, long totalDelta, long childrenTotalDelta) {
        baseProbe.endRequest(container, error, totalDelta, childrenTotalDelta);
    }

    protected final IRequest getRequest() {
        return baseProbe.superGetRequest();
    }

    protected final <T extends AbstractExitPointProbeCollector> T getCollector() {
        return (T) baseProbe.superGetCollector().getDelegate();
    }

    protected final boolean isRecursive() {
        return baseProbe.superIsRecursive();
    }

    protected final void setRecursive(boolean value) {
        baseProbe.superSetRecursive(value);
    }

    protected final long getStartTime() {
        return baseProbe.superGetStartTime();
    }

    protected final long getTimeDelta(long startTime) {
        return baseProbe.superGetTimeDelta(startTime);
    }
}
