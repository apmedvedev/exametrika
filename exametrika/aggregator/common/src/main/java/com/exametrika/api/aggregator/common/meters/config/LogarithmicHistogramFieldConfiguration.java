/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.meters.config;

import com.exametrika.api.aggregator.common.values.config.LogarithmicHistogramValueSchemaConfiguration;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link LogarithmicHistogramFieldConfiguration} is a configuration of logarithmic histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class LogarithmicHistogramFieldConfiguration extends HistogramFieldConfiguration {
    private final long minBound;

    public LogarithmicHistogramFieldConfiguration(long minBound, int binCount) {
        super(binCount);

        this.minBound = minBound;
    }

    public long getMinBound() {
        return minBound;
    }

    @Override
    public FieldValueSchemaConfiguration getSchema() {
        return new LogarithmicHistogramValueSchemaConfiguration(minBound, binCount);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LogarithmicHistogramFieldConfiguration))
            return false;

        LogarithmicHistogramFieldConfiguration configuration = (LogarithmicHistogramFieldConfiguration) o;
        return super.equals(o) && minBound == configuration.minBound;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(minBound);
    }
}
