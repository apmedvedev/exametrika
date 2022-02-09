/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import java.util.Set;

import com.exametrika.impl.aggregator.common.values.StackIdsAggregator;
import com.exametrika.impl.aggregator.common.values.StackIdsBuilder;
import com.exametrika.impl.aggregator.common.values.StackIdsSerializer;
import com.exametrika.spi.aggregator.common.values.IMetricAggregator;
import com.exametrika.spi.aggregator.common.values.IMetricValueBuilder;
import com.exametrika.spi.aggregator.common.values.IMetricValueSerializer;


/**
 * The {@link StackIdsValueSchemaConfiguration} is a stackIds value schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StackIdsValueSchemaConfiguration extends MetricValueSchemaConfiguration {
    public StackIdsValueSchemaConfiguration(String name) {
        super(name);
    }

    @Override
    public boolean isCompatible(MetricValueSchemaConfiguration metric) {
        return true;
    }

    @Override
    public IMetricValueBuilder createBuilder() {
        return new StackIdsBuilder();
    }

    @Override
    public IMetricValueSerializer createSerializer(boolean builder) {
        return new StackIdsSerializer(builder);
    }

    @Override
    public IMetricAggregator createAggregator() {
        return new StackIdsAggregator();
    }

    @Override
    public void buildBaseRepresentations(Set<String> baseRepresentations) {
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackIdsValueSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
