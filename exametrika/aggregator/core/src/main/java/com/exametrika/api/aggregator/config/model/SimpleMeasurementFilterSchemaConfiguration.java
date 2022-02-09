/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.common.utils.NameFilter;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.SimpleMeasurementFilter;
import com.exametrika.spi.aggregator.IMeasurementFilter;
import com.exametrika.spi.aggregator.config.model.MeasurementFilterSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link SimpleMeasurementFilterSchemaConfiguration} represents a configuration of simple measurement filter schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SimpleMeasurementFilterSchemaConfiguration extends MeasurementFilterSchemaConfiguration {
    private final NameFilter scopeFilter;
    private final NameFilter metricFilter;

    public SimpleMeasurementFilterSchemaConfiguration(NameFilter scopeFilter, NameFilter metricFilter) {
        this.scopeFilter = scopeFilter;
        this.metricFilter = metricFilter;
    }

    public NameFilter getScopeFilter() {
        return scopeFilter;
    }

    public NameFilter getMetricFilter() {
        return metricFilter;
    }

    @Override
    public IMeasurementFilter createFilter(IDatabaseContext context) {
        return new SimpleMeasurementFilter(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SimpleMeasurementFilterSchemaConfiguration))
            return false;

        SimpleMeasurementFilterSchemaConfiguration configuration = (SimpleMeasurementFilterSchemaConfiguration) o;
        return Objects.equals(scopeFilter, configuration.scopeFilter) && Objects.equals(metricFilter, configuration.metricFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(scopeFilter, metricFilter);
    }
}
