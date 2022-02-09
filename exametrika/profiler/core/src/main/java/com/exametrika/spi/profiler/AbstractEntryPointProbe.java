/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.profiler.probes.DelegatingEntryPointProbe;
import com.exametrika.spi.profiler.boot.ThreadLocalContainer;
import com.exametrika.spi.profiler.config.EntryPointProbeConfiguration;


/**
 * The {@link AbstractEntryPointProbe} is an entry point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AbstractEntryPointProbe extends AbstractProbe {
    private DelegatingEntryPointProbe baseProbe;

    public AbstractEntryPointProbe(EntryPointProbeConfiguration configuration, IProbeContext context) {
        super(configuration, context);
    }

    public final IProbeContext getContext() {
        return context;
    }

    public final void setBaseProbe(IProbe probe) {
        Assert.notNull(probe);

        this.baseProbe = (DelegatingEntryPointProbe) probe;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    public abstract AbstractEntryPointProbeCollector createCollector();

    public abstract IRequest mapScope(Object rawRequest);

    public IRequest mapRequest(IScope scope, Object rawRequest) {
        return baseProbe.superMapRequest(scope, rawRequest);
    }

    protected final void beginRequest(ThreadLocalContainer container, Object rawRequest, TraceTag tag) {
        baseProbe.beginRequest(container, rawRequest, tag);
    }

    protected final void endRequest(ThreadLocalContainer container, JsonObject error) {
        baseProbe.endRequest(container, error);
    }

    protected final IRequest getRequest() {
        return baseProbe.superGetRequest();
    }

    protected final <T extends AbstractEntryPointProbeCollector> T getCollector() {
        return (T) baseProbe.superGetCollector().getDelegate();
    }

    protected final boolean isRecursive() {
        return baseProbe.superIsRecursive();
    }

    protected final void setRecursive(boolean value) {
        baseProbe.superSetRecursive(value);
    }
}
