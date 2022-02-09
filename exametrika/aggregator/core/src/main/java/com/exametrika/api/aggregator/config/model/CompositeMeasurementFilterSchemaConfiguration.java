/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.aggregator.CompositeMeasurementFilter;
import com.exametrika.spi.aggregator.IMeasurementFilter;
import com.exametrika.spi.aggregator.config.model.MeasurementFilterSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link CompositeMeasurementFilterSchemaConfiguration} represents a configuration of composite measurement filter schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CompositeMeasurementFilterSchemaConfiguration extends MeasurementFilterSchemaConfiguration {
    private final List<MeasurementFilterSchemaConfiguration> filters;

    public CompositeMeasurementFilterSchemaConfiguration(List<MeasurementFilterSchemaConfiguration> filters) {
        Assert.notNull(filters);

        this.filters = Immutables.wrap(filters);
    }

    public List<MeasurementFilterSchemaConfiguration> getFilters() {
        return filters;
    }

    @Override
    public IMeasurementFilter createFilter(IDatabaseContext context) {
        List<IMeasurementFilter> filters = new ArrayList<IMeasurementFilter>(this.filters.size());
        for (MeasurementFilterSchemaConfiguration filter : this.filters)
            filters.add(filter.createFilter(context));

        return new CompositeMeasurementFilter(filters);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CompositeMeasurementFilterSchemaConfiguration))
            return false;

        CompositeMeasurementFilterSchemaConfiguration configuration = (CompositeMeasurementFilterSchemaConfiguration) o;
        return filters.equals(configuration.filters);
    }

    @Override
    public int hashCode() {
        return filters.hashCode();
    }
}
