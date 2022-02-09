/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import java.util.List;

import com.exametrika.api.aggregator.fields.IAggregationRecord;
import com.exametrika.api.aggregator.fields.IPeriodAggregationField;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IAggregationLogFilter;


/**
 * The {@link CompositeAggregationLogFilter} is an implementation of {@link IAggregationLogFilter}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class CompositeAggregationLogFilter implements IAggregationLogFilter {
    private final List<IAggregationLogFilter> filters;

    public CompositeAggregationLogFilter(List<IAggregationLogFilter> filters) {
        Assert.notNull(filters);

        this.filters = filters;
    }

    @Override
    public boolean allow(IPeriodAggregationField field, IAggregationRecord logRecord) {
        for (IAggregationLogFilter filter : filters) {
            if (filter.allow(field, logRecord))
                return true;
        }

        return false;
    }
}
