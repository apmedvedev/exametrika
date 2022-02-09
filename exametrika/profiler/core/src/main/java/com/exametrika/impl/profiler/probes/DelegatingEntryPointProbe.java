/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.profiler.AbstractEntryPointProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.TraceTag;
import com.exametrika.spi.profiler.boot.ThreadLocalContainer;
import com.exametrika.spi.profiler.config.EntryPointProbeConfiguration;


/**
 * The {@link DelegatingEntryPointProbe} is a delegating entry point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DelegatingEntryPointProbe extends EntryPointProbe {
    private final AbstractEntryPointProbe delegate;

    public DelegatingEntryPointProbe(EntryPointProbeConfiguration configuration, IProbeContext context, int index,
                                     AbstractEntryPointProbe delegate) {
        super(configuration, context, index);

        Assert.notNull(delegate);

        this.delegate = delegate;
        delegate.setBaseProbe(this);
    }

    @Override
    public void start() {
        super.start();

        delegate.start();
    }

    @Override
    public void stop() {
        super.stop();

        delegate.stop();
    }

    public void beginRequest(ThreadLocalContainer container, Object rawRequest, TraceTag tag) {
        super.beginRequest((Container) container, rawRequest, tag);
    }

    public void endRequest(ThreadLocalContainer container, JsonObject error) {
        super.endRequest((Container) container, error);
    }

    public IRequest superGetRequest() {
        return super.getRequest();
    }

    public IRequest superMapRequest(IScope scope, Object rawRequest) {
        return super.mapRequest(scope, rawRequest);
    }

    public DelegatingEntryPointProbeCollector superGetCollector() {
        return super.getCollector();
    }

    public boolean superIsRecursive() {
        return super.isRecursive();
    }

    public void superSetRecursive(boolean value) {
        super.setRecursive(value);
    }

    @Override
    protected EntryPointProbeCollector doCreateCollector(int index, String combineId,
                                                         ICallPath callPath, String name, StackProbeRootCollector root,
                                                         StackProbeCollector parent, JsonObject metadata, boolean primary, boolean leaf) {
        return new DelegatingEntryPointProbeCollector(index, this, name, combineId, callPath, root, parent, metadata, primary, leaf,
                delegate.createCollector());
    }

    @Override
    protected IRequest mapScope(Object rawRequest) {
        return delegate.mapScope(rawRequest);
    }

    @Override
    protected IRequest mapRequest(IScope scope, Object rawRequest) {
        return delegate.mapRequest(scope, rawRequest);
    }
}
