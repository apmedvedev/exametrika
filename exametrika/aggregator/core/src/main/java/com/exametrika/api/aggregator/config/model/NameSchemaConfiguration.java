/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.Collections;
import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.config.model.AggregationAnalyzerSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.AggregationFilterSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ComponentBindingStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ComponentDeletionStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ComponentDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MeasurementFilterSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MetricAggregationStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ScopeAggregationStrategySchemaConfiguration;


/**
 * The {@link NameSchemaConfiguration} is a aggregation name component type schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class NameSchemaConfiguration extends AggregationComponentTypeSchemaConfiguration {
    private final List<ScopeAggregationStrategySchemaConfiguration> scopeAggregationStrategies;
    private final List<MetricAggregationStrategySchemaConfiguration> metricAggregationStrategies;
    private final AggregationFilterSchemaConfiguration aggregationFilter;
    private final boolean allowHierarchyAggregation;
    private final boolean allowTransferDerived;
    private final List<ComponentDiscoveryStrategySchemaConfiguration> componentDiscoveryStrategies;
    private final ComponentDeletionStrategySchemaConfiguration componentDeletionStrategy;

    public NameSchemaConfiguration(String name, LogSchemaConfiguration metric,
                                   List<? extends ScopeAggregationStrategySchemaConfiguration> scopeAggregationStrategies,
                                   List<? extends MetricAggregationStrategySchemaConfiguration> metricAggregationStrategies,
                                   AggregationFilterSchemaConfiguration aggregationFilter, MeasurementFilterSchemaConfiguration filter,
                                   List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies, String ruleRepresentation,
                                   List<AggregationAnalyzerSchemaConfiguration> analyzers,
                                   boolean allowTransferDerived, boolean allowHierarchyAggregation) {
        this(name, Collections.singletonList(metric), true, scopeAggregationStrategies,
                metricAggregationStrategies, aggregationFilter, filter, componentBindingStrategies, ruleRepresentation, analyzers,
                allowTransferDerived, allowHierarchyAggregation, null, null);
    }

    public NameSchemaConfiguration(String name, List<? extends MetricTypeSchemaConfiguration> metricTypes, boolean hasLog,
                                   List<? extends ScopeAggregationStrategySchemaConfiguration> scopeAggregationStrategies,
                                   List<? extends MetricAggregationStrategySchemaConfiguration> metricAggregationStrategies,
                                   AggregationFilterSchemaConfiguration aggregationFilter, MeasurementFilterSchemaConfiguration filter,
                                   List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies, String ruleRepresentation,
                                   List<AggregationAnalyzerSchemaConfiguration> analyzers,
                                   boolean allowTransferDerived, boolean allowHierarchyAggregation,
                                   List<? extends ComponentDiscoveryStrategySchemaConfiguration> componentDiscoveryStrategies,
                                   ComponentDeletionStrategySchemaConfiguration componentDeletionStrategy) {
        super(name, metricTypes, hasLog, filter, componentBindingStrategies, ruleRepresentation, analyzers);

        Assert.notNull(scopeAggregationStrategies);
        Assert.notNull(metricAggregationStrategies);
        if (componentDiscoveryStrategies == null)
            componentDiscoveryStrategies = Collections.emptyList();

        this.allowHierarchyAggregation = allowHierarchyAggregation;
        this.allowTransferDerived = allowTransferDerived;
        this.scopeAggregationStrategies = Immutables.wrap(scopeAggregationStrategies);
        this.metricAggregationStrategies = Immutables.wrap(metricAggregationStrategies);
        this.aggregationFilter = aggregationFilter;
        this.componentDiscoveryStrategies = Immutables.wrap(componentDiscoveryStrategies);
        this.componentDeletionStrategy = componentDeletionStrategy;
    }

    @Override
    public Kind getKind() {
        return Kind.NAME;
    }

    public List<ScopeAggregationStrategySchemaConfiguration> getScopeAggregationStrategies() {
        return scopeAggregationStrategies;
    }

    public List<MetricAggregationStrategySchemaConfiguration> getMetricAggregationStrategies() {
        return metricAggregationStrategies;
    }

    public AggregationFilterSchemaConfiguration getAggregationFilter() {
        return aggregationFilter;
    }

    public boolean isAllowHierarchyAggregation() {
        return allowHierarchyAggregation;
    }

    public boolean isAllowTransferDerived() {
        return allowTransferDerived;
    }

    public List<ComponentDiscoveryStrategySchemaConfiguration> getComponentDiscoveryStrategies() {
        return componentDiscoveryStrategies;
    }

    public ComponentDeletionStrategySchemaConfiguration getComponentDeletionStrategy() {
        return componentDeletionStrategy;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NameSchemaConfiguration))
            return false;

        NameSchemaConfiguration configuration = (NameSchemaConfiguration) o;
        return super.equals(configuration) &&
                scopeAggregationStrategies.equals(configuration.scopeAggregationStrategies) &&
                metricAggregationStrategies.equals(configuration.metricAggregationStrategies) &&
                Objects.equals(aggregationFilter, configuration.aggregationFilter) &&
                allowTransferDerived == configuration.allowTransferDerived &&
                allowHierarchyAggregation == configuration.allowHierarchyAggregation &&
                componentDiscoveryStrategies.equals(configuration.componentDiscoveryStrategies) &&
                Objects.equals(componentDeletionStrategy, configuration.componentDeletionStrategy);
    }

    @Override
    public boolean equalsStructured(AggregationComponentTypeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof NameSchemaConfiguration))
            return false;

        NameSchemaConfiguration configuration = (NameSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(scopeAggregationStrategies, metricAggregationStrategies,
                aggregationFilter, allowTransferDerived, allowHierarchyAggregation, componentDiscoveryStrategies, componentDeletionStrategy);
    }
}
