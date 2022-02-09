/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.List;

import com.exametrika.spi.aggregator.config.model.AggregationAnalyzerSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ComponentBindingStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MeasurementFilterSchemaConfiguration;


/**
 * The {@link SecondaryEntryPointSchemaConfiguration} is a secondary entry point component type schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SecondaryEntryPointSchemaConfiguration extends EntryPointSchemaConfiguration {
    public SecondaryEntryPointSchemaConfiguration(String name, List<? extends MetricTypeSchemaConfiguration> metricTypes, boolean hasLog,
                                                  MeasurementFilterSchemaConfiguration filter, List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies,
                                                  String ruleRepresentation, List<AggregationAnalyzerSchemaConfiguration> analyzers, String stackNameComponentType) {
        super(name, metricTypes, hasLog, filter, componentBindingStrategies, ruleRepresentation, analyzers, stackNameComponentType);
    }

    @Override
    public Kind getKind() {
        return Kind.SECONDARY_ENTRY_POINT;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SecondaryEntryPointSchemaConfiguration))
            return false;

        SecondaryEntryPointSchemaConfiguration configuration = (SecondaryEntryPointSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(AggregationComponentTypeSchemaConfiguration newSchema) {
        if (!(newSchema instanceof SecondaryEntryPointSchemaConfiguration))
            return false;

        SecondaryEntryPointSchemaConfiguration configuration = (SecondaryEntryPointSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
