/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.Arrays;

import com.exametrika.api.aggregator.common.values.IHistogramValue;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link HistogramValue} is a measurement data for histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HistogramValue implements IHistogramValue {
    private final long[] bins;
    private final long minOutOfBounds;
    private final long maxOutOfBounds;

    public HistogramValue(long[] bins, long minOutOfBounds, long maxOutOfBounds) {
        Assert.notNull(bins);

        this.bins = bins;
        this.minOutOfBounds = minOutOfBounds;
        this.maxOutOfBounds = maxOutOfBounds;
    }

    @Override
    public int getBinCount() {
        return bins.length;
    }

    @Override
    public long getBin(int index) {
        return bins[index];
    }

    @Override
    public long getMinOutOfBounds() {
        return minOutOfBounds;
    }

    @Override
    public long getMaxOutOfBounds() {
        return maxOutOfBounds;
    }

    @Override
    public JsonObject toJson() {
        JsonArrayBuilder binList = new JsonArrayBuilder();
        for (int i = 0; i < bins.length; i++)
            binList.add(bins[i]);

        JsonObjectBuilder fields = new JsonObjectBuilder();
        fields.put("instanceOf", "histo");
        fields.put("bins", binList);
        fields.put("min-oob", minOutOfBounds);
        fields.put("max-oob", maxOutOfBounds);

        return fields.toJson();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HistogramValue))
            return false;

        HistogramValue data = (HistogramValue) o;
        return Arrays.equals(bins, data.bins) && minOutOfBounds == data.minOutOfBounds && maxOutOfBounds == data.maxOutOfBounds;
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(bins) + Objects.hashCode(minOutOfBounds, maxOutOfBounds);
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
