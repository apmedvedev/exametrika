/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link FieldMetricValueSchemaBuilder} is a builder for aggregation metric value schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class FieldMetricValueSchemaBuilder extends MetricValueSchemaBuilder {
    private final boolean stack;
    private final List<FieldValueSchemaConfiguration> fields = new ArrayList<FieldValueSchemaConfiguration>();

    public FieldMetricValueSchemaBuilder(ComponentValueSchemaBuilder parent, String name, boolean stack) {
        super(parent, name);

        this.stack = stack;
        fields.add(new StandardValueSchemaConfiguration());
    }

    public FieldMetricValueSchemaBuilder statistics() {
        fields.add(new StatisticsValueSchemaConfiguration());
        return this;
    }

    public FieldMetricValueSchemaBuilder uniformMistogram(long minBound, long maxBound, int binCount) {
        fields.add(new UniformHistogramValueSchemaConfiguration(minBound, maxBound, binCount));
        return this;
    }

    public FieldMetricValueSchemaBuilder logarithmicHistogram(long minBound, int binCount) {
        fields.add(new LogarithmicHistogramValueSchemaConfiguration(minBound, binCount));
        return this;
    }

    public FieldMetricValueSchemaBuilder customHistogram(List<Long> bounds) {
        fields.add(new CustomHistogramValueSchemaConfiguration(bounds));
        return this;
    }

    public FieldMetricValueSchemaBuilder instance(int instanceCount, boolean max) {
        fields.add(new InstanceValueSchemaConfiguration(instanceCount, max));
        return this;
    }

    @Override
    public MetricValueSchemaConfiguration toConfiguration() {
        if (!stack)
            return new NameValueSchemaConfiguration(name, fields);
        else
            return new StackValueSchemaConfiguration(name, fields);
    }
}
