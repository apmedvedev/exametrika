/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.spi.aggregator.config.model.BehaviorTypeLabelStrategySchemaConfiguration;


/**
 * The {@link ForecastValueSchemaConfiguration} is a forecast aggregation field schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ForecastValueSchemaConfiguration extends AnomalyValueSchemaConfiguration {
    public ForecastValueSchemaConfiguration(String name, String baseRepresentation, String baseField,
                                            boolean sensitivityAutoAdjustment, float initialSensitivity,
                                            float sensitivityIncrement, float maxSensitivity, int initialLearningPeriod, int initialAdjustmentLearningPeriod,
                                            int anomaliesEstimationPeriod, int maxAnomaliesPerEstimationPeriodPercentage, int maxAnomaliesPerType,
                                            boolean anomalyAutoLabeling, BehaviorTypeLabelStrategySchemaConfiguration behaviorTypeLabelStrategy) {
        super(name, baseRepresentation, baseField, false, sensitivityAutoAdjustment, initialSensitivity,
                sensitivityIncrement, maxSensitivity, initialLearningPeriod, initialAdjustmentLearningPeriod,
                anomaliesEstimationPeriod, maxAnomaliesPerEstimationPeriodPercentage, maxAnomaliesPerType, anomalyAutoLabeling,
                behaviorTypeLabelStrategy);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ForecastValueSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
