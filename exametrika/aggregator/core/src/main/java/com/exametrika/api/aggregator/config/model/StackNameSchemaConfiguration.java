/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;

import com.exametrika.spi.aggregator.config.model.AggregationAnalyzerSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.AggregationFilterSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ComponentBindingStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MeasurementFilterSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MetricAggregationStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ScopeAggregationStrategySchemaConfiguration;


/**
 * The {@link StackNameSchemaConfiguration} is a aggregation stack name component type schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackNameSchemaConfiguration extends NameSchemaConfiguration {
    public StackNameSchemaConfiguration(String name, List<? extends MetricTypeSchemaConfiguration> metricTypes, boolean hasLog,
                                        List<? extends ScopeAggregationStrategySchemaConfiguration> scopeAggregationStrategies,
                                        List<? extends MetricAggregationStrategySchemaConfiguration> metricAggregationStrategies,
                                        AggregationFilterSchemaConfiguration aggregationFilter, MeasurementFilterSchemaConfiguration filter,
                                        List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies, String ruleRepresentation,
                                        List<AggregationAnalyzerSchemaConfiguration> analyzers,
                                        boolean allowTransferDerived, boolean allowHierarchyAggregation) {
        super(name, metricTypes, hasLog, scopeAggregationStrategies, metricAggregationStrategies,
                aggregationFilter, filter, componentBindingStrategies, ruleRepresentation, analyzers,
                allowTransferDerived, allowHierarchyAggregation, null, null);
    }

    @Override
    public Kind getKind() {
        return Kind.STACK_NAME;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackNameSchemaConfiguration))
            return false;

        StackNameSchemaConfiguration configuration = (StackNameSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(AggregationComponentTypeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof StackNameSchemaConfiguration))
            return false;

        StackNameSchemaConfiguration configuration = (StackNameSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
