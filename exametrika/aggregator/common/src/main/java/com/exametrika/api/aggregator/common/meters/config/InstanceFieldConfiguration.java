/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.meters.config;


import com.exametrika.api.aggregator.common.values.config.InstanceValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.meters.IFieldFactory;
import com.exametrika.spi.aggregator.common.meters.config.FieldConfiguration;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link InstanceFieldConfiguration} is a configuration of instance fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstanceFieldConfiguration extends FieldConfiguration {
    private final int instanceCount;
    private final boolean max;

    public InstanceFieldConfiguration(int instanceCount, boolean max) {
        Assert.isTrue(instanceCount > 0);

        this.instanceCount = instanceCount;
        this.max = max;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public boolean isMax() {
        return max;
    }

    @Override
    public IFieldFactory createFactory() {
        Assert.supports(false);
        return null;
    }

    @Override
    public FieldValueSchemaConfiguration getSchema() {
        return new InstanceValueSchemaConfiguration(instanceCount, max);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof InstanceFieldConfiguration))
            return false;

        InstanceFieldConfiguration configuration = (InstanceFieldConfiguration) o;
        return instanceCount == configuration.instanceCount && max == configuration.max;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(instanceCount, max);
    }

    @Override
    public String toString() {
        return "instance";
    }
}
