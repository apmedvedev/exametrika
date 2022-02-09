/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import com.exametrika.common.utils.Assert;
import com.exametrika.impl.profiler.probes.DelegatingExitPointProbeCollector;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;
import com.exametrika.spi.profiler.boot.Collector;


/**
 * The {@link AbstractExitPointProbeCollector} is an exit point probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class AbstractExitPointProbeCollector {
    private final AbstractExitPointProbe probe;
    private DelegatingExitPointProbeCollector baseCollector;

    public AbstractExitPointProbeCollector(AbstractExitPointProbe probe) {
        Assert.notNull(probe);
        this.probe = probe;
    }

    public final void setBaseCollector(Collector collector) {
        Assert.notNull(baseCollector);

        this.baseCollector = (DelegatingExitPointProbeCollector) collector;
    }

    public final String getName() {
        return baseCollector.getName();
    }

    public final AbstractExitPointProbe getProbe() {
        return probe;
    }

    public void createMeters() {
    }

    public void clearMeters() {
    }

    public void beginMeasure(IRequest request) {
    }

    public void endMeasure(IRequest request) {
    }

    protected final IScope getScope() {
        return baseCollector.getScope();
    }

    protected final boolean isLeaf() {
        return baseCollector.isLeaf();
    }

    protected final MeterContainer getMeters() {
        return baseCollector.superGetMeters();
    }

    protected final ITransactionInfo getTransaction() {
        return baseCollector.getTransaction();
    }
}
