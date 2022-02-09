/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import java.util.Arrays;
import java.util.List;

import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.aggregator.common.values.HistogramBuilder;


/**
 * The {@link CustomHistogramValueSchemaConfiguration} is a configuration of custom-bounded histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CustomHistogramValueSchemaConfiguration extends HistogramValueSchemaConfiguration {
    private final List<Long> bounds;
    private final long[] boundsArray;

    public CustomHistogramValueSchemaConfiguration(List<Long> bounds) {
        super(bounds.size() - 1);

        this.bounds = Immutables.wrap(bounds);
        long[] boundsArray = new long[bounds.size()];
        for (int i = 0; i < bounds.size(); i++)
            boundsArray[i] = bounds.get(i);

        this.boundsArray = boundsArray;
    }

    public List<Long> getBounds() {
        return bounds;
    }

    @Override
    public void update(HistogramBuilder builder, long value) {
        int binIndex = Arrays.binarySearch(boundsArray, value);
        if (binIndex < 0)
            binIndex = -(binIndex + 2);

        if (binIndex == -1)
            builder.setMinOutOfBounds(builder.getMinOutOfBounds() + 1);
        else if (binIndex < builder.getBinCount())
            builder.getBins()[binIndex]++;
        else
            builder.setMaxOutOfBounds(builder.getMaxOutOfBounds() + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CustomHistogramValueSchemaConfiguration))
            return false;

        CustomHistogramValueSchemaConfiguration configuration = (CustomHistogramValueSchemaConfiguration) o;
        return super.equals(o) && bounds.equals(configuration.bounds);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + bounds.hashCode();
    }
}
