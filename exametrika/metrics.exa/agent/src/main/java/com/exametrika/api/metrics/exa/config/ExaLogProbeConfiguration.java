/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.exa.config;

import com.exametrika.impl.metrics.exa.probes.ExaLogProbe;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.LogProbeConfiguration;


/**
 * The {@link ExaLogProbeConfiguration} is a configuration of exa log probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaLogProbeConfiguration extends LogProbeConfiguration {
    public ExaLogProbeConfiguration(String name, String scopeType, long extractionPeriod, String measurementStrategy,
                                    long warmupDelay, LogConfiguration log) {
        super(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay, log);
    }

    @Override
    public String getComponentType() {
        return "exa.log";
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new ExaLogProbe(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExaLogProbeConfiguration))
            return false;

        ExaLogProbeConfiguration configuration = (ExaLogProbeConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
