/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.values.ComponentAccessorFactory;
import com.exametrika.impl.aggregator.values.ComponentComputer;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComponentComputer;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.IMetricComputer;


/**
 * The {@link ComponentRepresentationSchemaConfiguration} is a component representation schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ComponentRepresentationSchemaConfiguration extends Configuration {
    private final String name;
    private final Map<String, MetricRepresentationSchemaConfiguration> metrics;

    public ComponentRepresentationSchemaConfiguration(String name, Map<String, ? extends MetricRepresentationSchemaConfiguration> metrics) {
        Assert.notNull(name);
        Assert.notNull(metrics);

        this.name = name;

        this.metrics = Immutables.wrap(metrics);
    }

    public String getName() {
        return name;
    }

    public Map<String, MetricRepresentationSchemaConfiguration> getMetrics() {
        return metrics;
    }

    public IComponentComputer createComputer(ComponentValueSchemaConfiguration schema) {
        IComponentAccessorFactory componentAccessorFactory = createAccessorFactory(schema);
        List<IMetricComputer> computers = new ArrayList<IMetricComputer>(schema.getMetrics().size());
        List<String> metricTypeNames = new ArrayList<String>(schema.getMetrics().size());
        for (int i = 0; i < schema.getMetrics().size(); i++) {
            MetricValueSchemaConfiguration metricSchema = schema.getMetrics().get(i);
            MetricRepresentationSchemaConfiguration metric = metrics.get(metricSchema.getName());
            IMetricComputer computer = null;
            String metricTypeName = null;
            if (metric != null) {
                computer = metric.createComputer(schema, this, componentAccessorFactory, i);
                metricTypeName = metricSchema.getName();
            }

            computers.add(computer);
            metricTypeNames.add(metricTypeName);
        }

        return new ComponentComputer(computers, metricTypeNames);
    }

    public IComponentAccessorFactory createAccessorFactory(ComponentValueSchemaConfiguration schema) {
        Map<String, IMetricAccessorFactory> factoriesMap = new HashMap<String, IMetricAccessorFactory>();
        for (int i = 0; i < schema.getMetrics().size(); i++) {
            MetricValueSchemaConfiguration metricSchema = schema.getMetrics().get(i);
            MetricRepresentationSchemaConfiguration metric = metrics.get(metricSchema.getName());
            if (metric != null)
                factoriesMap.put(metricSchema.getName(), metric.createAccessorFactory(schema, this, i));
        }

        return new ComponentAccessorFactory(factoriesMap);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ComponentRepresentationSchemaConfiguration))
            return false;

        ComponentRepresentationSchemaConfiguration configuration = (ComponentRepresentationSchemaConfiguration) o;
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
