/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.meters.config;

import java.util.List;

import com.exametrika.api.aggregator.common.values.config.CustomHistogramValueSchemaConfiguration;
import com.exametrika.common.utils.Immutables;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link CustomHistogramFieldConfiguration} is a configuration of custom-bounded histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CustomHistogramFieldConfiguration extends HistogramFieldConfiguration {
    private final List<Long> bounds;

    public CustomHistogramFieldConfiguration(List<Long> bounds) {
        super(bounds.size() - 1);

        this.bounds = Immutables.wrap(bounds);
    }

    public List<Long> getBounds() {
        return bounds;
    }

    @Override
    public FieldValueSchemaConfiguration getSchema() {
        return new CustomHistogramValueSchemaConfiguration(bounds);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CustomHistogramFieldConfiguration))
            return false;

        CustomHistogramFieldConfiguration configuration = (CustomHistogramFieldConfiguration) o;
        return super.equals(o) && bounds.equals(configuration.bounds);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + bounds.hashCode();
    }
}
