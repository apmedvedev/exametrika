/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IStatisticsValue;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link StatisticsBuilder} is a measurement data for statistics fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class StatisticsBuilder implements IFieldValueBuilder, IStatisticsValue {
    private static final int CACHE_SIZE = Memory.getShallowSize(StatisticsBuilder.class);
    private double sumSquares;

    public StatisticsBuilder() {
    }

    public StatisticsBuilder(double sumSquares) {
        this.sumSquares = sumSquares;
    }

    @Override
    public double getSumSquares() {
        return sumSquares;
    }

    public void setSumSquares(double sumSquares) {
        this.sumSquares = sumSquares;
    }

    @Override
    public IJsonCollection toJson() {
        return toValue().toJson();
    }

    @Override
    public void set(IFieldValue value) {
        Assert.notNull(value);

        IStatisticsValue statisticsValue = (IStatisticsValue) value;

        sumSquares = statisticsValue.getSumSquares();
    }

    @Override
    public IFieldValue toValue() {
        return new StatisticsValue(sumSquares);
    }

    @Override
    public void clear() {
        sumSquares = 0d;
    }

    @Override
    public void normalizeEnd(long count) {
        sumSquares = sumSquares / count;
    }

    @Override
    public void normalizeDerived(FieldValueSchemaConfiguration fieldSchemaConfiguration, long sum) {
    }

    @Override
    public int getCacheSize() {
        return CACHE_SIZE;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StatisticsBuilder))
            return false;

        StatisticsBuilder data = (StatisticsBuilder) o;
        return sumSquares == data.sumSquares;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sumSquares);
    }

    @Override
    public String toString() {
        return toValue().toString();
    }
}
