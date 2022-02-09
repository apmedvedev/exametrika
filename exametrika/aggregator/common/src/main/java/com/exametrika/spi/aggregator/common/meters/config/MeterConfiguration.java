/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters.config;

import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Objects;


/**
 * The {@link MeterConfiguration} is a configuration of meter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class MeterConfiguration extends Configuration {
    private final boolean enabled;

    public MeterConfiguration(boolean enabled) {
        this.enabled = enabled;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public abstract MetricValueSchemaConfiguration getSchema(String metricType);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MeterConfiguration))
            return false;

        MeterConfiguration configuration = (MeterConfiguration) o;
        return enabled == configuration.enabled;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(enabled);
    }
}
