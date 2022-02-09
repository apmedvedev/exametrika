/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import com.exametrika.common.utils.Assert;
import com.exametrika.impl.profiler.probes.DelegatingEntryPointProbeCollector;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;
import com.exametrika.spi.profiler.boot.Collector;


/**
 * The {@link AbstractEntryPointProbeCollector} is an entry point probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class AbstractEntryPointProbeCollector {
    private final AbstractEntryPointProbe probe;
    private DelegatingEntryPointProbeCollector baseCollector;

    public AbstractEntryPointProbeCollector(AbstractEntryPointProbe probe) {
        Assert.notNull(probe);
        this.probe = probe;
    }

    public final void setBaseCollector(Collector collector) {
        Assert.isNull(baseCollector);

        this.baseCollector = (DelegatingEntryPointProbeCollector) collector;
    }

    public final String getName() {
        return baseCollector.getName();
    }

    public final AbstractEntryPointProbe getProbe() {
        return probe;
    }

    public void createMeters() {
    }

    public void clearMeters() {
    }

    public void beginMeasure(IRequest request, long currentThreadCpuTime) {
    }

    public void endMeasure(IRequest request, long currentThreadCpuTime) {
    }

    protected final IScope getScope() {
        return baseCollector.getScope();
    }

    protected final MeterContainer getMeters() {
        return baseCollector.superGetMeters();
    }

    protected final ITransactionInfo getTransaction() {
        return baseCollector.getTransaction();
    }
}
