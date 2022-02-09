/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.utils.Assert;


/**
 * The {@link ComponentValueSchemaBuilder} is a builder for aggregation component value schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ComponentValueSchemaBuilder {
    private final String name;
    private final List<Object> metrics = new ArrayList<Object>();

    public ComponentValueSchemaBuilder(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public FieldMetricValueSchemaBuilder nameFields(String name) {
        FieldMetricValueSchemaBuilder metric = new FieldMetricValueSchemaBuilder(this, name, false);
        metrics.add(metric);
        return metric;
    }

    public ComponentValueSchemaBuilder metric(MetricValueSchemaConfiguration metric) {
        metrics.add(metric);
        return this;
    }

    public ComponentValueSchemaBuilder metrics(List<MetricValueSchemaConfiguration> metrics) {
        this.metrics.addAll(metrics);
        return this;
    }

    public ComponentValueSchemaBuilder name(String name) {
        FieldMetricValueSchemaBuilder metric = new FieldMetricValueSchemaBuilder(this, name, false);
        metrics.add(metric);
        return this;
    }

    public FieldMetricValueSchemaBuilder stackFields(String name) {
        FieldMetricValueSchemaBuilder metric = new FieldMetricValueSchemaBuilder(this, name, true);
        metrics.add(metric);
        return metric;
    }

    public ComponentValueSchemaBuilder stack(String name) {
        FieldMetricValueSchemaBuilder metric = new FieldMetricValueSchemaBuilder(this, name, true);
        metrics.add(metric);
        return this;
    }

    public ComponentValueSchemaBuilder object(String name) {
        ObjectValueSchemaBuilder metric = new ObjectValueSchemaBuilder(this, name);
        metrics.add(metric);
        return this;
    }

    public ComponentValueSchemaConfiguration toConfiguration() {
        List<MetricValueSchemaConfiguration> metrics = new ArrayList<MetricValueSchemaConfiguration>(this.metrics.size());
        for (Object metric : this.metrics) {
            if (metric instanceof MetricValueSchemaBuilder)
                metrics.add(((MetricValueSchemaBuilder) metric).toConfiguration());
            else if (metric instanceof MetricValueSchemaConfiguration)
                metrics.add((MetricValueSchemaConfiguration) metric);
            else
                Assert.error();
        }

        return new ComponentValueSchemaConfiguration(name, metrics);
    }
}
