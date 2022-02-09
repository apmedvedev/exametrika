/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.impl.aggregator.common.values.NameAggregator;
import com.exametrika.impl.aggregator.common.values.NameBuilder;
import com.exametrika.impl.aggregator.common.values.NameSerializer;
import com.exametrika.spi.aggregator.common.values.IFieldAggregator;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;
import com.exametrika.spi.aggregator.common.values.IMetricAggregator;
import com.exametrika.spi.aggregator.common.values.IMetricValueBuilder;
import com.exametrika.spi.aggregator.common.values.IMetricValueSerializer;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link NameValueSchemaConfiguration} is a name value schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NameValueSchemaConfiguration extends FieldMetricValueSchemaConfiguration {
    public NameValueSchemaConfiguration(String name, List<? extends FieldValueSchemaConfiguration> fields) {
        super(name, fields);
    }

    @Override
    public IMetricValueBuilder createBuilder() {
        List<IFieldValueBuilder> builders = new ArrayList<IFieldValueBuilder>();
        for (FieldValueSchemaConfiguration field : getFields()) {
            IFieldValueBuilder builder = field.createBuilder();
            builders.add(builder);
        }

        return new NameBuilder(builders);
    }

    @Override
    public IMetricValueSerializer createSerializer(boolean builder) {
        List<IFieldValueSerializer> serializers = new ArrayList<IFieldValueSerializer>();
        for (FieldValueSchemaConfiguration field : getFields()) {
            IFieldValueSerializer serializer = field.createSerializer(builder);
            serializers.add(serializer);
        }

        return new NameSerializer(builder, serializers);
    }

    @Override
    public IMetricAggregator createAggregator() {
        List<IFieldAggregator> aggregators = new ArrayList<IFieldAggregator>();
        for (FieldValueSchemaConfiguration field : getFields()) {
            IFieldAggregator aggregator = field.createAggregator();
            aggregators.add(aggregator);
        }

        return new NameAggregator(aggregators);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NameValueSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
