/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.aggregator.CompositeAggregationFilter;
import com.exametrika.spi.aggregator.IAggregationFilter;
import com.exametrika.spi.aggregator.config.model.AggregationFilterSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link CompositeAggregationFilterSchemaConfiguration} represents a configuration of composite aggregation filter schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CompositeAggregationFilterSchemaConfiguration extends AggregationFilterSchemaConfiguration {
    private final List<AggregationFilterSchemaConfiguration> filters;

    public CompositeAggregationFilterSchemaConfiguration(List<AggregationFilterSchemaConfiguration> filters) {
        Assert.notNull(filters);

        this.filters = Immutables.wrap(filters);
    }

    public List<AggregationFilterSchemaConfiguration> getFilters() {
        return filters;
    }

    @Override
    public IAggregationFilter createFilter(IDatabaseContext context) {
        List<IAggregationFilter> filters = new ArrayList<IAggregationFilter>(this.filters.size());
        for (AggregationFilterSchemaConfiguration filter : this.filters)
            filters.add(filter.createFilter(context));

        return new CompositeAggregationFilter(filters);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CompositeAggregationFilterSchemaConfiguration))
            return false;

        CompositeAggregationFilterSchemaConfiguration configuration = (CompositeAggregationFilterSchemaConfiguration) o;
        return filters.equals(configuration.filters);
    }

    @Override
    public int hashCode() {
        return filters.hashCode();
    }
}
