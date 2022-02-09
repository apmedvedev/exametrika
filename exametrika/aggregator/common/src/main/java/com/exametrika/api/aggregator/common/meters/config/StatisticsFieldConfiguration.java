/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.meters.config;

import com.exametrika.api.aggregator.common.values.config.StatisticsValueSchemaConfiguration;
import com.exametrika.impl.aggregator.common.fields.statistics.StatisticsFieldFactory;
import com.exametrika.spi.aggregator.common.meters.IFieldFactory;
import com.exametrika.spi.aggregator.common.meters.config.FieldConfiguration;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link StatisticsFieldConfiguration} is a configuration of statistics fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StatisticsFieldConfiguration extends FieldConfiguration {
    @Override
    public IFieldFactory createFactory() {
        return new StatisticsFieldFactory();
    }

    @Override
    public FieldValueSchemaConfiguration getSchema() {
        return new StatisticsValueSchemaConfiguration();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StatisticsFieldConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "statistics";
    }
}
