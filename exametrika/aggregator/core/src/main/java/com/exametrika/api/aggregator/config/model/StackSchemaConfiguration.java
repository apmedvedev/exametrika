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
 * The {@link StackSchemaConfiguration} is a aggregation stack component type schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StackSchemaConfiguration extends AggregationComponentTypeSchemaConfiguration {
    private final String stackNameComponentType;

    public StackSchemaConfiguration(String name, List<? extends MetricTypeSchemaConfiguration> metricTypes, boolean hasLog,
                                    MeasurementFilterSchemaConfiguration filter, List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies,
                                    String ruleRepresentation, List<AggregationAnalyzerSchemaConfiguration> analyzers, String stackNameComponentType) {
        super(name, metricTypes, hasLog, filter, componentBindingStrategies, ruleRepresentation, analyzers);

        this.stackNameComponentType = stackNameComponentType;
    }

    @Override
    public Kind getKind() {
        return Kind.STACK;
    }

    public String getStackNameComponentType() {
        return stackNameComponentType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackSchemaConfiguration))
            return false;

        StackSchemaConfiguration configuration = (StackSchemaConfiguration) o;
        return super.equals(configuration) && Objects.equals(stackNameComponentType, configuration.stackNameComponentType);
    }

    @Override
    public boolean equalsStructured(AggregationComponentTypeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof StackSchemaConfiguration))
            return false;

        StackSchemaConfiguration configuration = (StackSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(stackNameComponentType);
    }
}
