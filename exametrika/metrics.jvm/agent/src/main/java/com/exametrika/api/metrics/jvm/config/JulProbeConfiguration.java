/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import com.exametrika.impl.metrics.jvm.probes.JulProbe;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.LogProbeConfiguration;


/**
 * The {@link JulProbeConfiguration} is a configuration of java.util.logging log probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JulProbeConfiguration extends LogProbeConfiguration {
    public JulProbeConfiguration(String name, String scopeType, long extractionPeriod, String measurementStrategy,
                                 long warmupDelay, LogConfiguration log) {
        super(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay, log);
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new JulProbe(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JulProbeConfiguration))
            return false;

        JulProbeConfiguration configuration = (JulProbeConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
