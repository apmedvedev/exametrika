/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link UniformHistogramRepresentationSchemaConfiguration} is a configuration of uniform histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class UniformHistogramRepresentationSchemaConfiguration extends HistogramRepresentationSchemaConfiguration {
    private final long minBound;
    private final long maxBound;
    private final JsonArray scale;

    public UniformHistogramRepresentationSchemaConfiguration(long minBound, long maxBound, int binCount,
                                                             boolean computeValues, boolean computePercentages, boolean computeCumulativePercentages, boolean computeScale,
                                                             List<Integer> percentiles, boolean enabled) {
        super(binCount, computeValues, computePercentages, computeCumulativePercentages, computeScale, percentiles, enabled);

        Assert.isTrue(minBound < maxBound);

        this.minBound = minBound;
        this.maxBound = maxBound;
        this.scale = computeScale();
    }

    public long getMinBound() {
        return minBound;
    }

    public long getMaxBound() {
        return maxBound;
    }

    @Override
    public JsonArray getScale() {
        return scale;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof UniformHistogramRepresentationSchemaConfiguration))
            return false;

        UniformHistogramRepresentationSchemaConfiguration configuration = (UniformHistogramRepresentationSchemaConfiguration) o;
        return super.equals(o) && minBound == configuration.minBound && maxBound == configuration.maxBound;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(minBound, maxBound);
    }

    private JsonArray computeScale() {
        long step = (maxBound - minBound) / getBinCount();
        JsonArrayBuilder builder = new JsonArrayBuilder();

        for (int i = 0; i < getBinCount(); i++)
            builder.add(minBound + i * step);

        builder.add(maxBound);

        return builder.toJson();
    }
}
