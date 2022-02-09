/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.utils.Objects;


/**
 * The {@link LogarithmicHistogramRepresentationSchemaConfiguration} is a configuration of logarithmic histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class LogarithmicHistogramRepresentationSchemaConfiguration extends HistogramRepresentationSchemaConfiguration {
    private final long minBound;
    private final JsonArray scale;

    public LogarithmicHistogramRepresentationSchemaConfiguration(long minBound, int binCount,
                                                                 boolean computeValues, boolean computePercentages, boolean computeCumulativePercentages, boolean computeScale,
                                                                 List<Integer> percentiles, boolean enabled) {
        super(binCount, computeValues, computePercentages, computeCumulativePercentages, computeScale, percentiles, enabled);

        this.minBound = minBound;
        this.scale = computeScale();
    }

    public long getMinBound() {
        return minBound;
    }

    @Override
    public JsonArray getScale() {
        return scale;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LogarithmicHistogramRepresentationSchemaConfiguration))
            return false;

        LogarithmicHistogramRepresentationSchemaConfiguration configuration = (LogarithmicHistogramRepresentationSchemaConfiguration) o;
        return super.equals(o) && minBound == configuration.minBound;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(minBound);
    }

    private JsonArray computeScale() {
        JsonArrayBuilder builder = new JsonArrayBuilder();

        long step = Math.max(1, minBound);
        for (int i = 0; i < getBinCount(); i++) {
            builder.add(step);
            step *= 2;
        }

        builder.add(step);

        return builder.toJson();
    }
}
