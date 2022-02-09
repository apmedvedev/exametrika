/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import java.util.List;

import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IMeasurementFilter;


/**
 * The {@link CompositeMeasurementFilter} is an implementation of {@link IMeasurementFilter}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class CompositeMeasurementFilter implements IMeasurementFilter {
    private final List<IMeasurementFilter> filters;

    public CompositeMeasurementFilter(List<IMeasurementFilter> filters) {
        Assert.notNull(filters);

        this.filters = filters;
    }

    @Override
    public boolean allow(Measurement measurement) {
        for (IMeasurementFilter filter : filters) {
            if (filter.allow(measurement))
                return true;
        }

        return false;
    }
}
