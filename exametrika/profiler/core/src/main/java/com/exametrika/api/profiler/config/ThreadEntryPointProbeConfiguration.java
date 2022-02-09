/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import com.exametrika.impl.profiler.probes.ThreadEntryPointProbe;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.EntryPointProbeConfiguration;


/**
 * The {@link ThreadEntryPointProbeConfiguration} is a configuration of thread entry point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ThreadEntryPointProbeConfiguration extends EntryPointProbeConfiguration {
    public ThreadEntryPointProbeConfiguration(String name, String scopeType, String measurementStrategy,
                                              long warmupDelay, long maxDuration, CounterConfiguration transactionTimeCounter, LogConfiguration stalledRequestsLog,
                                              PrimaryType allowPrimary, boolean allowSecondary, CounterConfiguration timeCounter, CounterConfiguration receiveBytesCounter,
                                              CounterConfiguration sendBytesCounter, LogConfiguration errorsLog) {
        super(name, scopeType, measurementStrategy, warmupDelay, null, maxDuration, transactionTimeCounter,
                stalledRequestsLog, null, null, allowPrimary, allowSecondary, timeCounter, receiveBytesCounter, sendBytesCounter, errorsLog);
    }

    @Override
    public String getType() {
        return super.getType() + ",thread";
    }

    @Override
    public String getEntryPointType() {
        return "threadEntry";
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new ThreadEntryPointProbe(this, context, index);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ThreadEntryPointProbeConfiguration))
            return false;

        ThreadEntryPointProbeConfiguration configuration = (ThreadEntryPointProbeConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
