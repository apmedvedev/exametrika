/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.common.values.ComponentAggregator;
import com.exametrika.impl.aggregator.common.values.ComponentBuilder;
import com.exametrika.impl.aggregator.common.values.ComponentSerializer;
import com.exametrika.spi.aggregator.common.values.IComponentAggregator;
import com.exametrika.spi.aggregator.common.values.IComponentValueBuilder;
import com.exametrika.spi.aggregator.common.values.IComponentValueSerializer;
import com.exametrika.spi.aggregator.common.values.IMetricAggregator;
import com.exametrika.spi.aggregator.common.values.IMetricValueBuilder;
import com.exametrika.spi.aggregator.common.values.IMetricValueSerializer;


/**
 * The {@link ComponentValueSchemaConfiguration} is a component value schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComponentValueSchemaConfiguration extends Configuration {
    private final String name;
    private final List<MetricValueSchemaConfiguration> metrics;
    private final Map<String, MetricValueSchemaConfiguration> metricsMap;

    public ComponentValueSchemaConfiguration(String name, List<? extends MetricValueSchemaConfiguration> metrics) {
        Assert.notNull(name);
        Assert.notNull(metrics);

        Map<String, MetricValueSchemaConfiguration> metricsMap = new HashMap<String, MetricValueSchemaConfiguration>();
        for (MetricValueSchemaConfiguration metric : metrics)
            Assert.isNull(metricsMap.put(metric.getName(), metric));

        this.name = name;
        this.metrics = Immutables.wrap(metrics);
        this.metricsMap = metricsMap;
    }

    public String getName() {
        return name;
    }

    public List<MetricValueSchemaConfiguration> getMetrics() {
        return metrics;
    }

    public MetricValueSchemaConfiguration findMetric(String name) {
        Assert.notNull(name);

        return metricsMap.get(name);
    }

    public boolean isCompatible(ComponentValueSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        int count = Math.min(metrics.size(), configuration.metrics.size());
        for (int i = 0; i < count; i++) {
            MetricValueSchemaConfiguration metric1 = metrics.get(i);
            MetricValueSchemaConfiguration metric2 = configuration.metrics.get(i);

            if (!metric1.isCompatible(metric2))
                return false;
        }

        return true;
    }

    public IComponentValueBuilder createBuilder() {
        List<IMetricValueBuilder> builders = new ArrayList<IMetricValueBuilder>();
        for (MetricValueSchemaConfiguration metric : metrics) {
            IMetricValueBuilder builder = metric.createBuilder();
            builders.add(builder);
        }

        return new ComponentBuilder(builders, null);
    }

    public IComponentValueSerializer createSerializer(boolean builder) {
        List<IMetricValueSerializer> serializers = new ArrayList<IMetricValueSerializer>();
        for (MetricValueSchemaConfiguration metric : metrics) {
            IMetricValueSerializer serializer = metric.createSerializer(builder);
            serializers.add(serializer);
        }

        return new ComponentSerializer(builder, serializers);
    }

    public IComponentAggregator createAggregator() {
        List<IMetricAggregator> aggregators = new ArrayList<IMetricAggregator>();
        for (MetricValueSchemaConfiguration metric : metrics) {
            IMetricAggregator aggregator = metric.createAggregator();
            aggregators.add(aggregator);
        }

        return new ComponentAggregator(aggregators);
    }

    public void buildBaseRepresentations(Set<String> baseRepresentations) {
        for (MetricValueSchemaConfiguration metric : metrics)
            metric.buildBaseRepresentations(baseRepresentations);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ComponentValueSchemaConfiguration))
            return false;

        ComponentValueSchemaConfiguration configuration = (ComponentValueSchemaConfiguration) o;
        return name.equals(configuration.name) && metrics.equals(configuration.metrics);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, metrics);
    }


    @Override
    public String toString() {
        return name;
    }
}
