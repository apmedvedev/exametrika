/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import com.exametrika.impl.aggregator.common.values.StandardAggregator;
import com.exametrika.impl.aggregator.common.values.StandardBuilder;
import com.exametrika.impl.aggregator.common.values.StandardSerializer;
import com.exametrika.spi.aggregator.common.values.IFieldAggregator;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link StandardValueSchemaConfiguration} is a standard aggregation field schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StandardValueSchemaConfiguration extends FieldValueSchemaConfiguration {
    public StandardValueSchemaConfiguration() {
        super("std");
    }

    @Override
    public String getBaseRepresentation() {
        return null;
    }

    @Override
    public IFieldValueSerializer createSerializer(boolean builder) {
        return new StandardSerializer(builder);
    }

    @Override
    public IFieldValueBuilder createBuilder() {
        return new StandardBuilder();
    }

    @Override
    public IFieldAggregator createAggregator() {
        return new StandardAggregator();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StandardValueSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
