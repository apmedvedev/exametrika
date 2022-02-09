/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters.config;

import java.util.List;


/**
 * The {@link GaugeConfiguration} is a configuration of gauge.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class GaugeConfiguration extends FieldMeterConfiguration {
    public GaugeConfiguration(boolean enabled) {
        super(enabled);
    }

    public GaugeConfiguration(boolean enabled, List<? extends FieldConfiguration> fields) {
        super(enabled, fields);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof GaugeConfiguration))
            return false;

        GaugeConfiguration configuration = (GaugeConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    public String toString() {
        return "gauge";
    }
}
