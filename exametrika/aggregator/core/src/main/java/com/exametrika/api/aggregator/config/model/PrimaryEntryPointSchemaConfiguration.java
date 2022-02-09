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
import com.exametrika.spi.aggregator.config.model.ComponentBindingStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ComponentDeletionStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ComponentDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MeasurementFilterSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ScopeAggregationStrategySchemaConfiguration;


/**
 * The {@link PrimaryEntryPointSchemaConfiguration} is a primary entry point component type schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PrimaryEntryPointSchemaConfiguration extends EntryPointSchemaConfiguration {
    private final List<ScopeAggregationStrategySchemaConfiguration> scopeAggregationStrategies;
    private final boolean allowHierarchyAggregation;
    private final boolean allowStackNameAggregation;
    private final String transactionFailureDependenciesComponentType;
    private final boolean allowTransactionFailureDependenciesAggregation;
    private final List<ComponentDiscoveryStrategySchemaConfiguration> componentDiscoveryStrategies;
    private final ComponentDeletionStrategySchemaConfiguration componentDeletionStrategy;
    private final boolean allowAnomaliesCorrelation;
    private final String anomaliesComponentType;

    public PrimaryEntryPointSchemaConfiguration(String name, List<? extends MetricTypeSchemaConfiguration> metricTypes, boolean hasLog,
                                                List<? extends ScopeAggregationStrategySchemaConfiguration> scopeAggregationStrategies, MeasurementFilterSchemaConfiguration filter,
                                                List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies, String ruleRepresentation,
                                                List<AggregationAnalyzerSchemaConfiguration> analyzers,
                                                String stackNameComponentType, boolean allowHierarchyAggregation, boolean allowStackNameAggregation,
                                                String transactionFailureDependenciesComponentType, boolean allowTransactionFailureDependenciesAggregation,
                                                List<ComponentDiscoveryStrategySchemaConfiguration> componentDiscoveryStrategies,
                                                ComponentDeletionStrategySchemaConfiguration componentDeletionStrategy,
                                                boolean allowAnomaliesCorrelation, String anomaliesComponentType) {
        super(name, metricTypes, hasLog, filter, componentBindingStrategies, ruleRepresentation, analyzers, stackNameComponentType);

        Assert.notNull(scopeAggregationStrategies);
        Assert.isTrue(!allowTransactionFailureDependenciesAggregation || transactionFailureDependenciesComponentType != null);
        Assert.isTrue(!allowAnomaliesCorrelation || anomaliesComponentType != null);

        if (componentDiscoveryStrategies == null)
            componentDiscoveryStrategies = Collections.emptyList();

        this.allowHierarchyAggregation = allowHierarchyAggregation;
        this.allowStackNameAggregation = allowStackNameAggregation;
        this.scopeAggregationStrategies = Immutables.wrap(scopeAggregationStrategies);
        this.transactionFailureDependenciesComponentType = transactionFailureDependenciesComponentType;
        this.allowTransactionFailureDependenciesAggregation = allowTransactionFailureDependenciesAggregation;
        this.componentDiscoveryStrategies = Immutables.wrap(componentDiscoveryStrategies);
        this.componentDeletionStrategy = componentDeletionStrategy;
        this.allowAnomaliesCorrelation = allowAnomaliesCorrelation;
        this.anomaliesComponentType = anomaliesComponentType;
    }

    @Override
    public Kind getKind() {
        return Kind.PRIMARY_ENTRY_POINT;
    }

    public List<ScopeAggregationStrategySchemaConfiguration> getScopeAggregationStrategies() {
        return scopeAggregationStrategies;
    }

    public boolean isAllowHierarchyAggregation() {
        return allowHierarchyAggregation;
    }

    public boolean isAllowStackNameAggregation() {
        return allowStackNameAggregation;
    }

    public String getTransactionFailureDependenciesComponentType() {
        return transactionFailureDependenciesComponentType;
    }

    public boolean isAllowTransactionFailureDependenciesAggregation() {
        return allowTransactionFailureDependenciesAggregation;
    }

    public List<ComponentDiscoveryStrategySchemaConfiguration> getComponentDiscoveryStrategies() {
        return componentDiscoveryStrategies;
    }

    public ComponentDeletionStrategySchemaConfiguration getComponentDeletionStrategy() {
        return componentDeletionStrategy;
    }

    public boolean isAllowAnomaliesCorrelation() {
        return allowAnomaliesCorrelation;
    }

    public String getAnomaliesComponentType() {
        return anomaliesComponentType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PrimaryEntryPointSchemaConfiguration))
            return false;

        PrimaryEntryPointSchemaConfiguration configuration = (PrimaryEntryPointSchemaConfiguration) o;
        return super.equals(configuration) &&
                scopeAggregationStrategies.equals(configuration.scopeAggregationStrategies) &&
                allowHierarchyAggregation == configuration.allowHierarchyAggregation &&
                allowStackNameAggregation == configuration.allowStackNameAggregation &&
                Objects.equals(transactionFailureDependenciesComponentType, configuration.transactionFailureDependenciesComponentType) &&
                allowTransactionFailureDependenciesAggregation == configuration.allowTransactionFailureDependenciesAggregation &&
                componentDiscoveryStrategies.equals(configuration.componentDiscoveryStrategies) &&
                Objects.equals(componentDeletionStrategy, configuration.componentDeletionStrategy) &&
                allowAnomaliesCorrelation == configuration.allowAnomaliesCorrelation &&
                Objects.equals(anomaliesComponentType, configuration.anomaliesComponentType);
    }

    @Override
    public boolean equalsStructured(AggregationComponentTypeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof PrimaryEntryPointSchemaConfiguration))
            return false;

        PrimaryEntryPointSchemaConfiguration configuration = (PrimaryEntryPointSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(scopeAggregationStrategies, allowHierarchyAggregation,
                allowStackNameAggregation, transactionFailureDependenciesComponentType,
                allowTransactionFailureDependenciesAggregation, componentDiscoveryStrategies, componentDeletionStrategy,
                allowAnomaliesCorrelation, anomaliesComponentType);
    }
}
