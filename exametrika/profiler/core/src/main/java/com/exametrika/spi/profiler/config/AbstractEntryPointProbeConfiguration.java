/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler.config;

import com.exametrika.impl.profiler.probes.DelegatingEntryPointProbe;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.profiler.AbstractEntryPointProbe;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;


/**
 * The {@link AbstractEntryPointProbeConfiguration} is a configuration of entry point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AbstractEntryPointProbeConfiguration extends EntryPointProbeConfiguration {
    public AbstractEntryPointProbeConfiguration(String name, String scopeType, String measurementStrategy,
                                                long warmupDelay, RequestMappingStrategyConfiguration requestMappingStrategy, long maxDuration,
                                                CounterConfiguration transactionTimeCounter, LogConfiguration stalledRequestsLog, String primaryEntryPointExpression,
                                                String stackMeasurementStrategy, PrimaryType allowPrimary, boolean allowSecondary,
                                                CounterConfiguration timeCounter, CounterConfiguration receiveBytesCounter,
                                                CounterConfiguration sendBytesCounter, LogConfiguration errorsLog) {
        super(name, scopeType, measurementStrategy, warmupDelay, requestMappingStrategy, maxDuration, transactionTimeCounter,
                stalledRequestsLog, primaryEntryPointExpression, stackMeasurementStrategy, allowPrimary, allowSecondary,
                timeCounter, receiveBytesCounter, sendBytesCounter, errorsLog);
    }

    @Override
    public final IProbe createProbe(int index, IProbeContext context) {
        return new DelegatingEntryPointProbe(this, context, index, doCreateProbe(context));
    }

    protected abstract AbstractEntryPointProbe doCreateProbe(IProbeContext context);
}
