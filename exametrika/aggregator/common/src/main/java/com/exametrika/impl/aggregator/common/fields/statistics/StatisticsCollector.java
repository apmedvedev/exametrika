/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.fields.statistics;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.impl.aggregator.common.values.StatisticsValue;
import com.exametrika.spi.aggregator.common.meters.IFieldCollector;


/**
 * The {@link StatisticsCollector} is an implementation of {@link IFieldCollector} for statistics fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StatisticsCollector implements IFieldCollector {
    private double sumSquares;

    @Override
    public void update(long value) {
        sumSquares += value * value;
    }

    @Override
    public IFieldValue extract(long count, double approximationMultiplier, boolean clear) {
        double sumSquares;
        if (approximationMultiplier > 0)
            sumSquares = this.sumSquares * approximationMultiplier;
        else
            sumSquares = this.sumSquares;

        IFieldValue value = new StatisticsValue(sumSquares);

        if (clear)
            this.sumSquares = 0d;

        return value;
    }
}
