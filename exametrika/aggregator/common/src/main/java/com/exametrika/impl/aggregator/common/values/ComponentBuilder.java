/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.values.IComponentValueBuilder;
import com.exametrika.spi.aggregator.common.values.IMetricValueBuilder;


/**
 * The {@link ComponentBuilder} is a measurement value builder for component measurements.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ComponentBuilder implements IComponentValueBuilder, IComponentValue {
    private static final int CACHE_SIZE = Memory.getShallowSize(ComponentBuilder.class);
    private final List<IMetricValueBuilder> metrics;
    private JsonObject metadata;

    public ComponentBuilder(List<IMetricValueBuilder> metrics, JsonObject metadata) {
        Assert.notNull(metrics);

        this.metrics = Immutables.wrap(metrics);
        this.metadata = metadata;
    }

    @Override
    public List<IMetricValueBuilder> getMetrics() {
        return metrics;
    }

    @Override
    public JsonObject getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(JsonObject metadata) {
        this.metadata = metadata;
    }

    @Override
    public IJsonCollection toJson() {
        return toValue().toJson();
    }

    @Override
    public IJsonCollection toJson(List<String> metricTypes, JsonObject metadata) {
        return toValue().toJson(metricTypes, metadata);
    }

    @Override
    public void set(IComponentValue value) {
        Assert.notNull(value);

        metadata = value.getMetadata();
        int count = Math.min(metrics.size(), value.getMetrics().size());
        for (int i = 0; i < count; i++) {
            IMetricValueBuilder builder = metrics.get(i);
            IMetricValue metric = value.getMetrics().get(i);
            if (metric != null)
                builder.set(metric);
        }
    }

    @Override
    public IComponentValue toValue() {
        return toValue(true);
    }

    @Override
    public IComponentValue toValue(boolean includeMetadata) {
        List<IMetricValue> metrics = new ArrayList<IMetricValue>(this.metrics.size());
        for (IMetricValueBuilder field : this.metrics)
            metrics.add(field.toValue());

        return new ComponentValue(metrics, includeMetadata ? metadata : null);
    }

    @Override
    public void clear() {
        for (int i = 0; i < metrics.size(); i++)
            metrics.get(i).clear();
    }

    @Override
    public int getCacheSize() {
        int cacheSize = 0;
        for (int i = 0; i < metrics.size(); i++)
            cacheSize += metrics.get(i).getCacheSize();

        return CACHE_SIZE + CacheSizes.getArrayListCacheSize(metrics) + cacheSize;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ComponentBuilder))
            return false;

        ComponentBuilder data = (ComponentBuilder) o;
        return metrics.equals(data.metrics) && Objects.equals(metadata, data.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(metrics, metadata);
    }

    @Override
    public String toString() {
        return toValue().toString();
    }
}
