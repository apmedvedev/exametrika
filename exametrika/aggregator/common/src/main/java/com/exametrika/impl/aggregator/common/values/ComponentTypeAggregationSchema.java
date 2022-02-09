/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.values.IComponentTypeAggregationSchema;
import com.exametrika.spi.aggregator.common.values.IComponentAggregator;
import com.exametrika.spi.aggregator.common.values.IComponentValueSerializer;


/**
 * The {@link ComponentTypeAggregationSchema} is an implementation of {@link IComponentTypeAggregationSchema}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComponentTypeAggregationSchema implements IComponentTypeAggregationSchema {
    private final ComponentValueSchemaConfiguration configuration;
    private final IComponentAggregator aggregator;
    private final IComponentValueSerializer valueSerializer;
    private final IComponentValueSerializer builderSerializer;

    public ComponentTypeAggregationSchema(ComponentValueSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
        this.aggregator = configuration.createAggregator();
        this.valueSerializer = configuration.createSerializer(false);
        this.builderSerializer = configuration.createSerializer(true);
    }

    @Override
    public ComponentValueSchemaConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public IComponentAggregator getAggregator() {
        return aggregator;
    }

    @Override
    public IComponentValueSerializer getValueSerializer() {
        return valueSerializer;
    }

    @Override
    public IComponentValueSerializer getBuilderSerializer() {
        return builderSerializer;
    }

    @Override
    public String toString() {
        return configuration.toString();
    }
}
