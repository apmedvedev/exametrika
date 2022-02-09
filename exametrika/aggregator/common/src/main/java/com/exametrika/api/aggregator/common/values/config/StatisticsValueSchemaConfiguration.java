/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import com.exametrika.impl.aggregator.common.values.StatisticsAggregator;
import com.exametrika.impl.aggregator.common.values.StatisticsBuilder;
import com.exametrika.impl.aggregator.common.values.StatisticsSerializer;
import com.exametrika.spi.aggregator.common.values.IFieldAggregator;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link StatisticsValueSchemaConfiguration} is a statistics aggergation field schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StatisticsValueSchemaConfiguration extends FieldValueSchemaConfiguration {
    public StatisticsValueSchemaConfiguration() {
        super("stat");
    }

    @Override
    public String getBaseRepresentation() {
        return null;
    }

    @Override
    public IFieldValueSerializer createSerializer(boolean builder) {
        return new StatisticsSerializer(builder);
    }

    @Override
    public IFieldValueBuilder createBuilder() {
        return new StatisticsBuilder();
    }

    @Override
    public IFieldAggregator createAggregator() {
        return new StatisticsAggregator();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StatisticsValueSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
