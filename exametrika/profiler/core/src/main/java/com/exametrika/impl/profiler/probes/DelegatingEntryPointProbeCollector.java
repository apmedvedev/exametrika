/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;
import com.exametrika.spi.profiler.AbstractEntryPointProbeCollector;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.ITransactionInfo;


/**
 * The {@link DelegatingEntryPointProbeCollector} is an entry point probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DelegatingEntryPointProbeCollector extends EntryPointProbeCollector {
    private final AbstractEntryPointProbeCollector delegate;

    public DelegatingEntryPointProbeCollector(int index, EntryPointProbe probe, String name, String combineId, ICallPath callPath,
                                              StackProbeRootCollector root, StackProbeCollector parent, JsonObject metadata, boolean primary, boolean leaf,
                                              AbstractEntryPointProbeCollector delegate) {
        super(index, probe, name, combineId, callPath, root, parent, metadata, primary, leaf);

        Assert.notNull(delegate);

        this.delegate = delegate;
        delegate.setBaseCollector(this);
    }

    public AbstractEntryPointProbeCollector getDelegate() {
        return delegate;
    }

    public IScope getScope() {
        return getRoot().getScope();
    }

    public MeterContainer superGetMeters() {
        return super.getMeters();
    }

    public ITransactionInfo getTransaction() {
        return getRoot().getTransaction();
    }

    @Override
    protected void doCreateMeters() {
        delegate.createMeters();
    }

    @Override
    protected void doClearMeters() {
        delegate.clearMeters();
    }

    @Override
    protected void doBeginMeasure(IRequest request, long currentThreadCpuTime) {
        delegate.beginMeasure(request, currentThreadCpuTime);
    }

    @Override
    protected void doEndMeasure(IRequest request, long currentThreadCpuTime) {
        delegate.endMeasure(request, currentThreadCpuTime);
    }
}
