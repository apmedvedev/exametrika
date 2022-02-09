/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Immutables;


/**
 * The {@link CustomHistogramRepresentationSchemaConfiguration} is a configuration of custom-bounded histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CustomHistogramRepresentationSchemaConfiguration extends HistogramRepresentationSchemaConfiguration {
    private final List<Long> bounds;
    private final JsonArray scale;

    public CustomHistogramRepresentationSchemaConfiguration(List<Long> bounds,
                                                            boolean computeValues, boolean computePercentages, boolean computeCumulativePercentages, boolean computeScale,
                                                            List<Integer> percentiles, boolean enabled) {
        super(bounds.size() - 1, computeValues, computePercentages, computeCumulativePercentages, computeScale,
                percentiles, enabled);

        this.bounds = Immutables.wrap(bounds);
        this.scale = JsonUtils.toJson(bounds);
    }

    public List<Long> getBounds() {
        return bounds;
    }

    @Override
    public JsonArray getScale() {
        return scale;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CustomHistogramRepresentationSchemaConfiguration))
            return false;

        CustomHistogramRepresentationSchemaConfiguration configuration = (CustomHistogramRepresentationSchemaConfiguration) o;
        return super.equals(o) && bounds.equals(configuration.bounds);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + bounds.hashCode();
    }
}
