/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;

import com.exametrika.spi.aggregator.config.model.AggregationAnalyzerSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ComponentBindingStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MeasurementFilterSchemaConfiguration;


/**
 * The {@link IntermediateExitPointSchemaConfiguration} is a intermediate exit point component type schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class IntermediateExitPointSchemaConfiguration extends ExitPointSchemaConfiguration {
    public IntermediateExitPointSchemaConfiguration(String name, List<? extends MetricTypeSchemaConfiguration> metricTypes, boolean hasLog,
                                                    MeasurementFilterSchemaConfiguration filter, List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies,
                                                    String ruleRepresentation, List<AggregationAnalyzerSchemaConfiguration> analyzers, String stackNameComponentType) {
        super(name, metricTypes, hasLog, filter, componentBindingStrategies, ruleRepresentation, analyzers, stackNameComponentType);
    }

    @Override
    public Kind getKind() {
        return Kind.INTERMEDIATE_EXIT_POINT;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof IntermediateExitPointSchemaConfiguration))
            return false;

        IntermediateExitPointSchemaConfiguration configuration = (IntermediateExitPointSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(AggregationComponentTypeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof IntermediateExitPointSchemaConfiguration))
            return false;

        IntermediateExitPointSchemaConfiguration configuration = (IntermediateExitPointSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
