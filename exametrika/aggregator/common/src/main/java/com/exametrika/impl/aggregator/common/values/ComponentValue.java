/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.List;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;


/**
 * The {@link ComponentValue} is a measurement value for component measurements.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComponentValue implements IComponentValue {
    private final List<IMetricValue> metrics;
    private final JsonObject metadata;

    public ComponentValue(List<? extends IMetricValue> metrics, JsonObject metadata) {
        Assert.notNull(metrics);

        this.metrics = Immutables.wrap(metrics);
        this.metadata = metadata;
    }

    @Override
    public List<IMetricValue> getMetrics() {
        return metrics;
    }

    @Override
    public JsonObject getMetadata() {
        return metadata;
    }

    @Override
    public JsonObject toJson() {
        JsonObjectBuilder metrics = new JsonObjectBuilder();
        metrics.put("metrics", toJsonArray(this.metrics));

        if (metadata != null)
            metrics.put("metadata", metadata);
        return metrics.toJson();
    }

    @Override
    public JsonObject toJson(List<String> metricTypes, JsonObject metadata) {
        JsonObjectBuilder metrics = new JsonObjectBuilder();
        metrics.put("metrics", toJsonObject(this.metrics, metricTypes));

        if (metadata != null)
            metrics.put("metadata", metadata);
        return metrics.toJson();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ComponentValue))
            return false;

        ComponentValue data = (ComponentValue) o;
        return metrics.equals(data.metrics) && Objects.equals(metadata, data.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(metrics, metadata);
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    private JsonArray toJsonArray(List<IMetricValue> metricList) {
        JsonArrayBuilder metrics = new JsonArrayBuilder();
        for (IMetricValue metric : metricList) {
            if (metric != null)
                metrics.add(metric.toJson());
            else
                metrics.add(null);
        }

        return metrics.toJson();
    }

    private JsonObject toJsonObject(List<IMetricValue> metricList, List<String> metricTypes) {
        Assert.isTrue(metricList.size() == metricTypes.size());

        JsonObjectBuilder metrics = new JsonObjectBuilder();
        for (int i = 0; i < metricList.size(); i++) {
            IMetricValue metric = metricList.get(i);
            if (metric != null) {
                String metricType = metricTypes.get(i);
                metrics.put(metricType, metric.toJson());
            }
        }

        return metrics.toJson();
    }
}
