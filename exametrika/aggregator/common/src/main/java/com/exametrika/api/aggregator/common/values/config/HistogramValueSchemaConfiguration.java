/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.common.values.HistogramAggregator;
import com.exametrika.impl.aggregator.common.values.HistogramBuilder;
import com.exametrika.impl.aggregator.common.values.HistogramSerializer;
import com.exametrika.spi.aggregator.common.values.IFieldAggregator;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link HistogramValueSchemaConfiguration} is a histogram aggregation field schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class HistogramValueSchemaConfiguration extends FieldValueSchemaConfiguration {
    private final int binCount;

    public HistogramValueSchemaConfiguration(int binCount) {
        super("histo");

        Assert.isTrue(binCount > 0);

        this.binCount = binCount;
    }

    @Override
    public String getBaseRepresentation() {
        return null;
    }

    public int getBinCount() {
        return binCount;
    }

    @Override
    public IFieldValueBuilder createBuilder() {
        return new HistogramBuilder(binCount);
    }

    @Override
    public IFieldValueSerializer createSerializer(boolean builder) {
        return new HistogramSerializer(builder, binCount);
    }

    @Override
    public IFieldAggregator createAggregator() {
        return new HistogramAggregator(binCount);
    }

    public abstract void update(HistogramBuilder builder, long value);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HistogramValueSchemaConfiguration))
            return false;

        HistogramValueSchemaConfiguration configuration = (HistogramValueSchemaConfiguration) o;
        return super.equals(o) && binCount == configuration.binCount;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(binCount);
    }
}
