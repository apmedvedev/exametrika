/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;
import com.exametrika.spi.profiler.AbstractExitPointProbeCollector;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.ITransactionInfo;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;


/**
 * The {@link DelegatingExitPointProbeCollector} is an exit point probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DelegatingExitPointProbeCollector extends ExitPointProbeCollector {
    private final AbstractExitPointProbeCollector delegate;

    public DelegatingExitPointProbeCollector(ExitPointProbeConfiguration configuration, int index, String name, UUID stackId,
                                             ICallPath callPath, StackProbeRootCollector root, StackProbeCollector parent, JsonObject metadata,
                                             ExitPointProbeCalibrateInfo calibrateInfo, boolean leaf, AbstractExitPointProbeCollector delegate) {
        super(configuration, index, name, stackId, callPath, root, parent, metadata, calibrateInfo, leaf);

        Assert.notNull(delegate);

        this.delegate = delegate;
        delegate.setBaseCollector(this);
    }

    public AbstractExitPointProbeCollector getDelegate() {
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
    protected void doBeginMeasure(IRequest request) {
        delegate.beginMeasure(request);
    }

    @Override
    protected void doEndMeasure(IRequest request) {
        delegate.endMeasure(request);
    }
}
