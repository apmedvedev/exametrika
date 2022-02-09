/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import com.exametrika.impl.metrics.jvm.probes.JdbcRequestGroupingStrategy;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequestGroupingStrategy;
import com.exametrika.spi.profiler.config.RequestGroupingStrategyConfiguration;


/**
 * The {@link JdbcRequestGroupingStrategyConfiguration} is a configuration of jdbc request grouping strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JdbcRequestGroupingStrategyConfiguration extends RequestGroupingStrategyConfiguration {
    @Override
    public IRequestGroupingStrategy createStrategy(IProbeContext context) {
        return new JdbcRequestGroupingStrategy();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JdbcRequestGroupingStrategyConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return 31 * getClass().hashCode();
    }
}
