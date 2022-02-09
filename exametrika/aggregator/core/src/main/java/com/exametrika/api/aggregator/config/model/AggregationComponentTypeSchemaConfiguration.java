/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.config.model.AggregationAnalyzerSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ComponentBindingStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MeasurementFilterSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;


/**
 * The {@link AggregationComponentTypeSchemaConfiguration} is a aggregation component type schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AggregationComponentTypeSchemaConfiguration extends SchemaConfiguration {
    private final List<MetricTypeSchemaConfiguration> metricTypes;
    private final Map<String, MetricTypeSchemaConfiguration> metricTypesMap;
    private final boolean log;
    private final boolean hasLog;
    private final ComponentValueSchemaConfiguration metrics;
    private final List<ComponentRepresentationSchemaConfiguration> representations;
    private final Map<String, ComponentRepresentationSchemaConfiguration> representationsMap;
    private final MeasurementFilterSchemaConfiguration filter;
    private final List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies;
    private final String ruleRepresentation;
    private final List<AggregationAnalyzerSchemaConfiguration> analyzers;

    public enum Kind {
        NAME,
        STACK_NAME,
        STACK,
        BACKGROUND_ROOT,
        PRIMARY_ENTRY_POINT,
        SECONDARY_ENTRY_POINT,
        EXIT_POINT,
        INTERMEDIATE_EXIT_POINT,
        STACK_LOG,
        STACK_ERROR_LOG
    }

    public AggregationComponentTypeSchemaConfiguration(String name, LogSchemaConfiguration metric, MeasurementFilterSchemaConfiguration filter) {
        this(name, Collections.singletonList(metric), true, filter, null, null, null);
    }

    public AggregationComponentTypeSchemaConfiguration(String name, List<? extends MetricTypeSchemaConfiguration> metricTypes, boolean hasLog,
                                                       MeasurementFilterSchemaConfiguration filter, List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies,
                                                       String ruleRepresentation, List<AggregationAnalyzerSchemaConfiguration> analyzers) {
        super(name, name, null);

        Assert.notNull(name);
        Assert.notNull(metricTypes);

        if (componentBindingStrategies != null && !componentBindingStrategies.isEmpty())
            Assert.isTrue(ruleRepresentation != null);

        boolean log = false;
        List<MetricValueSchemaConfiguration> metrics = new ArrayList<MetricValueSchemaConfiguration>();
        Map<String, MetricTypeSchemaConfiguration> metricTypesMap = new HashMap<String, MetricTypeSchemaConfiguration>();
        for (MetricTypeSchemaConfiguration metricType : metricTypes) {
            if (metricType instanceof LogSchemaConfiguration)
                log = true;

            Assert.isNull(metricTypesMap.put(metricType.getName(), metricType));
            metrics.add(metricType.getFields());
        }

        Assert.isTrue(!log || (metricTypes.size() == 1 && hasLog));
        this.log = log;

        this.metrics = new ComponentValueSchemaConfiguration(name, metrics);

        List<ComponentRepresentationSchemaConfiguration> representations = buildRepresentations(metricTypes);
        Map<String, ComponentRepresentationSchemaConfiguration> representationsMap = new HashMap<String, ComponentRepresentationSchemaConfiguration>();
        for (ComponentRepresentationSchemaConfiguration field : representations)
            Assert.isNull(representationsMap.put(field.getName(), field));

        this.representations = Immutables.wrap(representations);
        this.representationsMap = representationsMap;
        this.metricTypes = Immutables.wrap(metricTypes);
        this.metricTypesMap = metricTypesMap;
        this.hasLog = hasLog;
        this.filter = filter;
        this.componentBindingStrategies = Immutables.wrap(com.exametrika.common.utils.Collections.notNull(componentBindingStrategies));
        this.ruleRepresentation = ruleRepresentation;
        this.analyzers = Immutables.wrap(com.exametrika.common.utils.Collections.notNull(analyzers));
    }

    public abstract Kind getKind();

    public List<MetricTypeSchemaConfiguration> getMetricTypes() {
        return metricTypes;
    }

    public MetricTypeSchemaConfiguration findMetricType(String name) {
        Assert.notNull(name);

        return metricTypesMap.get(name);
    }

    public ComponentValueSchemaConfiguration getMetrics() {
        return metrics;
    }

    public List<ComponentRepresentationSchemaConfiguration> getRepresentations() {
        return representations;
    }

    public ComponentRepresentationSchemaConfiguration findRepresentation(String name) {
        Assert.notNull(name);

        return representationsMap.get(name);
    }

    public boolean isLog() {
        return log;
    }

    public boolean hasLog() {
        return hasLog;
    }

    public MeasurementFilterSchemaConfiguration getFilter() {
        return filter;
    }

    public List<ComponentBindingStrategySchemaConfiguration> getComponentBindingStrategies() {
        return componentBindingStrategies;
    }

    public String getRuleRepresentation() {
        return ruleRepresentation;
    }

    public List<AggregationAnalyzerSchemaConfiguration> getAnalyzers() {
        return analyzers;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AggregationComponentTypeSchemaConfiguration))
            return false;

        AggregationComponentTypeSchemaConfiguration configuration = (AggregationComponentTypeSchemaConfiguration) o;
        return super.equals(configuration) && metricTypes.equals(configuration.metricTypes) &&
                representations.equals(configuration.representations) && hasLog == configuration.hasLog &&
                Objects.equals(filter, configuration.filter) && componentBindingStrategies.equals(configuration.componentBindingStrategies) &&
                Objects.equals(ruleRepresentation, configuration.ruleRepresentation) && analyzers.equals(configuration.analyzers);
    }

    public boolean equalsStructured(AggregationComponentTypeSchemaConfiguration newSchema) {
        return getName().equals(newSchema.getName()) && metricTypes.equals(newSchema.metricTypes) &&
                representations.equals(newSchema.representations) && hasLog == newSchema.hasLog;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(metricTypes, representations, hasLog, filter, componentBindingStrategies,
                ruleRepresentation, analyzers);
    }

    private List<ComponentRepresentationSchemaConfiguration> buildRepresentations(List<? extends MetricTypeSchemaConfiguration> metricTypes) {
        Map<String, Map<String, MetricRepresentationSchemaConfiguration>> componentsMap = new LinkedHashMap<String, Map<String, MetricRepresentationSchemaConfiguration>>();
        for (MetricTypeSchemaConfiguration metricType : metricTypes) {
            for (MetricRepresentationSchemaConfiguration representation : metricType.getRepresentations()) {
                Map<String, MetricRepresentationSchemaConfiguration> metricsMap = componentsMap.get(representation.getName());
                if (metricsMap == null) {
                    metricsMap = new LinkedHashMap<String, MetricRepresentationSchemaConfiguration>();
                    componentsMap.put(representation.getName(), metricsMap);
                }

                metricsMap.put(metricType.getName(), representation);
            }
        }

        List<ComponentRepresentationSchemaConfiguration> representations = new ArrayList<ComponentRepresentationSchemaConfiguration>();
        for (Map.Entry<String, Map<String, MetricRepresentationSchemaConfiguration>> entry : componentsMap.entrySet()) {
            ComponentRepresentationSchemaConfiguration representation = new ComponentRepresentationSchemaConfiguration(entry.getKey(),
                    entry.getValue());
            representations.add(representation);
        }

        return representations;
    }
}
