/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import java.util.List;

import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IAggregationFilter;


/**
 * The {@link CompositeAggregationFilter} is an implementation of {@link IAggregationFilter}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class CompositeAggregationFilter implements IAggregationFilter {
    private final List<IAggregationFilter> filters;

    public CompositeAggregationFilter(List<IAggregationFilter> filters) {
        Assert.notNull(filters);

        this.filters = filters;
    }

    @Override
    public boolean deny(IScopeName scope, IMetricName metric) {
        for (IAggregationFilter filter : filters) {
            if (filter.deny(scope, metric))
                return false;
        }

        return true;
    }
}
