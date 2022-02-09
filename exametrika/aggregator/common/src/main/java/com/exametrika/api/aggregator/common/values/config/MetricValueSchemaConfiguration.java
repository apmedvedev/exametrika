/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import java.util.Set;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.values.IMetricAggregator;
import com.exametrika.spi.aggregator.common.values.IMetricValueBuilder;
import com.exametrika.spi.aggregator.common.values.IMetricValueSerializer;


/**
 * The {@link MetricValueSchemaConfiguration} is a aggregation metric value schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class MetricValueSchemaConfiguration extends Configuration {
    private final String name;

    public MetricValueSchemaConfiguration(String name) {
        Assert.notNull(name);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract boolean isCompatible(MetricValueSchemaConfiguration metric);

    public abstract IMetricValueBuilder createBuilder();

    public abstract IMetricValueSerializer createSerializer(boolean builder);

    public abstract IMetricAggregator createAggregator();

    public abstract void buildBaseRepresentations(Set<String> baseRepresentations);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MetricValueSchemaConfiguration))
            return false;

        MetricValueSchemaConfiguration configuration = (MetricValueSchemaConfiguration) o;
        return name.equals(configuration.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
