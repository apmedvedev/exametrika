/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.config.model.AggregationAnalyzerSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ComponentBindingStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MeasurementFilterSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ScopeAggregationStrategySchemaConfiguration;


/**
 * The {@link BackgroundRootSchemaConfiguration} is a background root component type schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class BackgroundRootSchemaConfiguration extends EntryPointSchemaConfiguration {
    private final List<ScopeAggregationStrategySchemaConfiguration> scopeAggregationStrategies;
    private final boolean allowHierarchyAggregation;
    private final boolean allowStackNameAggregation;
    private final boolean allowAnomaliesCorrelation;
    private final String anomaliesComponentType;

    public BackgroundRootSchemaConfiguration(String name, List<? extends MetricTypeSchemaConfiguration> metricTypes, boolean hasLog,
                                             List<? extends ScopeAggregationStrategySchemaConfiguration> scopeAggregationStrategies,
                                             MeasurementFilterSchemaConfiguration filter, List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies,
                                             String ruleRepresentation, List<AggregationAnalyzerSchemaConfiguration> analyzers, String stackNameComponentType,
                                             boolean allowHierarchyAggregation, boolean allowStackNameAggregation, boolean allowAnomaliesCorrelation, String anomaliesComponentType) {
        super(name, metricTypes, hasLog, filter, componentBindingStrategies, ruleRepresentation, analyzers, stackNameComponentType);

        Assert.notNull(scopeAggregationStrategies);
        Assert.isTrue(!allowAnomaliesCorrelation || anomaliesComponentType != null);

        this.allowHierarchyAggregation = allowHierarchyAggregation;
        this.allowStackNameAggregation = allowStackNameAggregation;
        this.scopeAggregationStrategies = Immutables.wrap(scopeAggregationStrategies);
        this.allowAnomaliesCorrelation = allowAnomaliesCorrelation;
        this.anomaliesComponentType = anomaliesComponentType;
    }

    @Override
    public Kind getKind() {
        return Kind.BACKGROUND_ROOT;
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
        if (!(o instanceof BackgroundRootSchemaConfiguration))
            return false;

        BackgroundRootSchemaConfiguration configuration = (BackgroundRootSchemaConfiguration) o;
        return super.equals(configuration) &&
                scopeAggregationStrategies.equals(configuration.scopeAggregationStrategies) &&
                allowHierarchyAggregation == configuration.allowHierarchyAggregation &&
                allowStackNameAggregation == configuration.allowStackNameAggregation &&
                allowAnomaliesCorrelation == configuration.allowAnomaliesCorrelation &&
                Objects.equals(anomaliesComponentType, configuration.anomaliesComponentType);
    }

    @Override
    public boolean equalsStructured(AggregationComponentTypeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof BackgroundRootSchemaConfiguration))
            return false;

        BackgroundRootSchemaConfiguration configuration = (BackgroundRootSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(scopeAggregationStrategies, allowHierarchyAggregation,
                allowStackNameAggregation, allowAnomaliesCorrelation, anomaliesComponentType);
    }
}
