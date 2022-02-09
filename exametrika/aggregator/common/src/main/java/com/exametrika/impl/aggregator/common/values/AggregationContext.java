/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.spi.aggregator.common.values.IAggregationContext;


/**
 * The {@link AggregationContext} is an implementation of {@link IAggregationContext}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class AggregationContext implements IAggregationContext {
    private boolean allowTotal = true;
    private boolean derived;
    private long time;
    private long period;
    private boolean aggregateMetadata;

    @Override
    public boolean isAllowTotal() {
        return allowTotal;
    }

    public void setAllowTotal(boolean allowTotal) {
        this.allowTotal = allowTotal;
    }

    @Override
    public boolean isDerived() {
        return derived;
    }

    public void setDerived(boolean derived) {
        this.derived = derived;
    }

    @Override
    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    @Override
    public boolean isAggregateMetadata() {
        return aggregateMetadata;
    }

    public void setAggregateMetadata(boolean value) {
        aggregateMetadata = value;
    }

    public void reset() {
        allowTotal = true;
        derived = false;
        time = 0;
        period = 0;
        aggregateMetadata = false;
    }
}
