/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.aggregator.CompositeAggregationLogFilter;
import com.exametrika.spi.aggregator.IAggregationLogFilter;
import com.exametrika.spi.aggregator.config.model.AggregationLogFilterSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link CompositeAggregationLogFilterSchemaConfiguration} represents a configuration of composite aggregation log filter schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CompositeAggregationLogFilterSchemaConfiguration extends AggregationLogFilterSchemaConfiguration {
    private final List<AggregationLogFilterSchemaConfiguration> filters;

    public CompositeAggregationLogFilterSchemaConfiguration(List<AggregationLogFilterSchemaConfiguration> filters) {
        Assert.notNull(filters);

        this.filters = Immutables.wrap(filters);
    }

    public List<AggregationLogFilterSchemaConfiguration> getFilters() {
        return filters;
    }

    @Override
    public IAggregationLogFilter createFilter(IDatabaseContext context) {
        List<IAggregationLogFilter> filters = new ArrayList<IAggregationLogFilter>(this.filters.size());
        for (AggregationLogFilterSchemaConfiguration filter : this.filters)
            filters.add(filter.createFilter(context));

        return new CompositeAggregationLogFilter(filters);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CompositeAggregationLogFilterSchemaConfiguration))
            return false;

        CompositeAggregationLogFilterSchemaConfiguration configuration = (CompositeAggregationLogFilterSchemaConfiguration) o;
        return filters.equals(configuration.filters);
    }

    @Override
    public int hashCode() {
        return filters.hashCode();
    }
}
