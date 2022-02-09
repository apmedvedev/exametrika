/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values;

import java.util.List;

import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.json.JsonObject;

/**
 * The {@link IComponentValue} represents an immutable measurement top-level component value.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface IComponentValue {
    /**
     * Returns metrics.
     *
     * @return metrics
     */
    List<? extends IMetricValue> getMetrics();

    /**
     * Returns metadata.
     *
     * @return metadata or null if metadata are not set
     */
    JsonObject getMetadata();

    IJsonCollection toJson();

    IJsonCollection toJson(List<String> metricTypes, JsonObject metadata);

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
