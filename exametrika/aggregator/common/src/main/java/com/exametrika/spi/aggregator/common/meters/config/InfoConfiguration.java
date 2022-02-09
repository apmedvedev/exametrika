/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters.config;

import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ObjectValueSchemaConfiguration;


/**
 * The {@link InfoConfiguration} is a configuration of informational meter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class InfoConfiguration extends MeterConfiguration {
    public InfoConfiguration(boolean enabled) {
        super(enabled);
    }

    @Override
    public MetricValueSchemaConfiguration getSchema(String metricType) {
        return new ObjectValueSchemaConfiguration(metricType);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof InfoConfiguration))
            return false;

        InfoConfiguration configuration = (InfoConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    public String toString() {
        return "info";
    }
}
