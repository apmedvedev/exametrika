/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler.config;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.StackValueSchemaConfiguration;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.config.FieldConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.FieldMeterConfiguration;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.profiler.IProbeContext;


/**
 * The {@link StackCounterConfiguration} is a configuration of stack counter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class StackCounterConfiguration extends FieldMeterConfiguration {
    public StackCounterConfiguration(boolean enabled) {
        super(enabled);
    }

    public StackCounterConfiguration(boolean enabled, List<? extends FieldConfiguration> fields) {
        super(enabled, fields);
    }

    public abstract String getMetricType();

    public abstract boolean isFast();

    public abstract IMeasurementProvider createProvider(IProbeContext context);

    public MetricValueSchemaConfiguration getSchema() {
        return getSchema(getMetricType());
    }

    @Override
    public MetricValueSchemaConfiguration getSchema(String metricType) {
        List<FieldValueSchemaConfiguration> fields = new ArrayList<FieldValueSchemaConfiguration>(getFields().size());
        for (FieldConfiguration field : getFields())
            fields.add(field.getSchema());
        return new StackValueSchemaConfiguration(metricType, fields);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackCounterConfiguration))
            return false;

        StackCounterConfiguration configuration = (StackCounterConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    public String toString() {
        return "stackCounter";
    }
}
