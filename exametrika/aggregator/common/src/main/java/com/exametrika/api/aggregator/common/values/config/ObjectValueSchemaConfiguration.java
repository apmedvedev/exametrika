/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import java.util.Set;

import com.exametrika.impl.aggregator.common.values.ObjectAggregator;
import com.exametrika.impl.aggregator.common.values.ObjectBuilder;
import com.exametrika.impl.aggregator.common.values.ObjectSerializer;
import com.exametrika.spi.aggregator.common.values.IMetricAggregator;
import com.exametrika.spi.aggregator.common.values.IMetricValueBuilder;
import com.exametrika.spi.aggregator.common.values.IMetricValueSerializer;


/**
 * The {@link ObjectValueSchemaConfiguration} is a object value schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ObjectValueSchemaConfiguration extends MetricValueSchemaConfiguration {
    public ObjectValueSchemaConfiguration(String name) {
        super(name);
    }

    @Override
    public boolean isCompatible(MetricValueSchemaConfiguration metric) {
        return true;
    }

    @Override
    public IMetricValueBuilder createBuilder() {
        return new ObjectBuilder();
    }

    @Override
    public IMetricValueSerializer createSerializer(boolean builder) {
        return new ObjectSerializer(builder);
    }

    @Override
    public IMetricAggregator createAggregator() {
        return new ObjectAggregator();
    }

    @Override
    public void buildBaseRepresentations(Set<String> baseRepresentations) {
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ObjectValueSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
