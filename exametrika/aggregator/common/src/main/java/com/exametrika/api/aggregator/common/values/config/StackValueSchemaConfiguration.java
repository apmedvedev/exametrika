/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.impl.aggregator.common.values.StackAggregator;
import com.exametrika.impl.aggregator.common.values.StackBuilder;
import com.exametrika.impl.aggregator.common.values.StackSerializer;
import com.exametrika.spi.aggregator.common.values.IFieldAggregator;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;
import com.exametrika.spi.aggregator.common.values.IMetricAggregator;
import com.exametrika.spi.aggregator.common.values.IMetricValueBuilder;
import com.exametrika.spi.aggregator.common.values.IMetricValueSerializer;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link StackValueSchemaConfiguration} is a stack value schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackValueSchemaConfiguration extends FieldMetricValueSchemaConfiguration {
    public StackValueSchemaConfiguration(String name, List<? extends FieldValueSchemaConfiguration> fields) {
        super(name, fields);
    }

    @Override
    public IMetricValueBuilder createBuilder() {
        List<IFieldValueBuilder> inherentBuilders = new ArrayList<IFieldValueBuilder>();
        List<IFieldValueBuilder> totalBuilders = new ArrayList<IFieldValueBuilder>();
        for (FieldValueSchemaConfiguration field : getFields()) {
            IFieldValueBuilder builder = field.createBuilder();
            inherentBuilders.add(builder);

            builder = field.createBuilder();
            totalBuilders.add(builder);
        }

        return new StackBuilder(inherentBuilders, totalBuilders);
    }

    @Override
    public IMetricValueSerializer createSerializer(boolean builder) {
        List<IFieldValueSerializer> serializers = new ArrayList<IFieldValueSerializer>();
        for (FieldValueSchemaConfiguration field : getFields()) {
            IFieldValueSerializer serializer = field.createSerializer(builder);
            serializers.add(serializer);
        }

        return new StackSerializer(builder, serializers);
    }

    @Override
    public IMetricAggregator createAggregator() {
        List<IFieldAggregator> aggregators = new ArrayList<IFieldAggregator>();
        for (FieldValueSchemaConfiguration field : getFields()) {
            IFieldAggregator aggregator = field.createAggregator();
            aggregators.add(aggregator);
        }

        return new StackAggregator(aggregators);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackValueSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
