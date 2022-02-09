/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;

import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.config.model.AggregationAnalyzerSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ComponentBindingStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MeasurementFilterSchemaConfiguration;


/**
 * The {@link StackLogSchemaConfiguration} is a stack log component type schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StackLogSchemaConfiguration extends AggregationComponentTypeSchemaConfiguration {
    private final boolean allowHierarchyAggregation;

    public StackLogSchemaConfiguration(String name, List<? extends MetricTypeSchemaConfiguration> metricTypes, boolean hasLog,
                                       MeasurementFilterSchemaConfiguration filter, List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies,
                                       String ruleRepresentation, List<AggregationAnalyzerSchemaConfiguration> analyzers, boolean allowHierarchyAggregation) {
        super(name, metricTypes, hasLog, filter, componentBindingStrategies, ruleRepresentation, analyzers);

        this.allowHierarchyAggregation = allowHierarchyAggregation;
    }

    @Override
    public Kind getKind() {
        return Kind.STACK_LOG;
    }

    public boolean isAllowHierarchyAggregation() {
        return allowHierarchyAggregation;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackLogSchemaConfiguration))
            return false;

        StackLogSchemaConfiguration configuration = (StackLogSchemaConfiguration) o;
        return super.equals(configuration) &&
                allowHierarchyAggregation == configuration.allowHierarchyAggregation;
    }

    @Override
    public boolean equalsStructured(AggregationComponentTypeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof StackLogSchemaConfiguration))
            return false;

        StackLogSchemaConfiguration configuration = (StackLogSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(allowHierarchyAggregation);
    }
}
