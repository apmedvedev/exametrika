/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.common.values.InstanceAggregator;
import com.exametrika.impl.aggregator.common.values.InstanceBuilder;
import com.exametrika.impl.aggregator.common.values.InstanceSerializer;
import com.exametrika.spi.aggregator.common.values.IFieldAggregator;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link InstanceValueSchemaConfiguration} is a configuration of instance aggregation field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstanceValueSchemaConfiguration extends FieldValueSchemaConfiguration {
    private final int instanceCount;
    private final boolean max;

    public InstanceValueSchemaConfiguration(int instanceCount, boolean max) {
        super("instance");

        Assert.isTrue(instanceCount > 0);

        this.instanceCount = instanceCount;
        this.max = max;
    }

    @Override
    public String getBaseRepresentation() {
        return null;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public boolean isMax() {
        return max;
    }

    @Override
    public IFieldValueBuilder createBuilder() {
        return new InstanceBuilder(max);
    }

    @Override
    public IFieldValueSerializer createSerializer(boolean builder) {
        return new InstanceSerializer(builder, max);
    }

    @Override
    public IFieldAggregator createAggregator() {
        return new InstanceAggregator(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof InstanceValueSchemaConfiguration))
            return false;

        InstanceValueSchemaConfiguration configuration = (InstanceValueSchemaConfiguration) o;
        return super.equals(o) && instanceCount == configuration.instanceCount && max == configuration.max;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(instanceCount, max);
    }
}
