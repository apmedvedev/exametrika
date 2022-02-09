/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.values.AnomalyAggregator;
import com.exametrika.impl.aggregator.values.AnomalyBuilder;
import com.exametrika.impl.aggregator.values.AnomalySerializer;
import com.exametrika.spi.aggregator.common.values.IFieldAggregator;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.BehaviorTypeLabelStrategySchemaConfiguration;


/**
 * The {@link AnomalyValueSchemaConfiguration} is a anomaly aggregation field schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AnomalyValueSchemaConfiguration extends FieldValueSchemaConfiguration {
    private final String baseRepresentation;
    private final String baseField;
    private final boolean fast;
    private final boolean sensitivityAutoAdjustment;
    private final float initialSensitivity;
    private final float sensitivityIncrement;
    private final float maxSensitivity;
    private final int initialLearningPeriod;
    private final int initialAdjustmentLearningPeriod;
    private final int anomaliesEstimationPeriod;
    private final int maxAnomaliesPerEstimationPeriodPercentage;
    private final int maxAnomaliestPerType;
    private final boolean anomalyAutoLabeling;
    private final BehaviorTypeLabelStrategySchemaConfiguration behaviorTypeLabelStrategy;

    public AnomalyValueSchemaConfiguration(String name, String baseRepresentation, String baseField,
                                           boolean fast, boolean sensitivityAutoAdjustment, float initialSensitivity,
                                           float sensitivityIncrement, float maxSensitivity, int initialLearningPeriod, int initialAdjustmentLearningPeriod,
                                           int anomaliesEstimationPeriod, int maxAnomaliesPerEstimationPeriodPercentage, int maxAnomaliesPerType,
                                           boolean anomalyAutoLabeling, BehaviorTypeLabelStrategySchemaConfiguration behaviorTypeLabelStrategy) {
        super(name);

        Assert.notNull(baseRepresentation);
        Assert.notNull(baseField);
        Assert.isTrue(initialSensitivity > 0 && initialSensitivity < 1);
        Assert.isTrue(initialSensitivity < maxSensitivity);
        Assert.isTrue(maxSensitivity < 1);
        Assert.isTrue(sensitivityIncrement < 1);
        Assert.isTrue(initialLearningPeriod >= 50);
        Assert.isTrue(initialAdjustmentLearningPeriod >= 50);
        Assert.isTrue(initialAdjustmentLearningPeriod <= initialLearningPeriod);
        Assert.isTrue(maxAnomaliesPerEstimationPeriodPercentage <= 100);

        this.baseRepresentation = baseRepresentation;
        this.baseField = baseField;
        this.fast = fast;
        this.sensitivityAutoAdjustment = sensitivityAutoAdjustment;
        this.initialSensitivity = initialSensitivity;
        this.sensitivityIncrement = sensitivityIncrement;
        this.maxSensitivity = maxSensitivity;
        this.initialLearningPeriod = initialLearningPeriod;
        this.initialAdjustmentLearningPeriod = initialAdjustmentLearningPeriod;
        this.anomaliesEstimationPeriod = anomaliesEstimationPeriod;
        this.maxAnomaliesPerEstimationPeriodPercentage = maxAnomaliesPerEstimationPeriodPercentage;
        this.maxAnomaliestPerType = maxAnomaliesPerType;
        this.anomalyAutoLabeling = anomalyAutoLabeling;
        this.behaviorTypeLabelStrategy = behaviorTypeLabelStrategy;
    }

    @Override
    public String getBaseRepresentation() {
        return baseRepresentation;
    }

    public String getBaseField() {
        return baseField;
    }

    public boolean isFast() {
        return fast;
    }

    public boolean isSensitivityAutoAdjustment() {
        return sensitivityAutoAdjustment;
    }

    public float getInitialSensitivity() {
        return initialSensitivity;
    }

    public float getSensitivityIncrement() {
        return sensitivityIncrement;
    }

    public float getMaxSensitivity() {
        return maxSensitivity;
    }

    public int getInitialLearningPeriod() {
        return initialLearningPeriod;
    }

    public int getInitialAdjustmentLearningPeriod() {
        return initialAdjustmentLearningPeriod;
    }

    public int getAnomaliesEstimationPeriod() {
        return anomaliesEstimationPeriod;
    }

    public int getMaxAnomaliesPerEstimationPeriodPercentage() {
        return maxAnomaliesPerEstimationPeriodPercentage;
    }

    public int getMaxAnomaliesPerType() {
        return maxAnomaliestPerType;
    }

    public boolean isAnomalyAutoLabeling() {
        return anomalyAutoLabeling;
    }

    public BehaviorTypeLabelStrategySchemaConfiguration getBahaviorTypeLabelStrategy() {
        return behaviorTypeLabelStrategy;
    }

    @Override
    public IFieldValueSerializer createSerializer(boolean builder) {
        return new AnomalySerializer(builder);
    }

    @Override
    public IFieldValueBuilder createBuilder() {
        return new AnomalyBuilder();
    }

    @Override
    public IFieldAggregator createAggregator() {
        return new AnomalyAggregator();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AnomalyValueSchemaConfiguration))
            return false;

        AnomalyValueSchemaConfiguration configuration = (AnomalyValueSchemaConfiguration) o;
        return super.equals(o) && baseRepresentation.equals(configuration.baseRepresentation) && baseField.equals(configuration.baseField) &&
                fast == configuration.fast && sensitivityAutoAdjustment == configuration.sensitivityAutoAdjustment &&
                initialSensitivity == configuration.initialSensitivity && sensitivityIncrement == configuration.sensitivityIncrement &&
                maxSensitivity == configuration.maxSensitivity && initialLearningPeriod == configuration.initialLearningPeriod &&
                initialAdjustmentLearningPeriod == configuration.initialAdjustmentLearningPeriod &&
                anomaliesEstimationPeriod == configuration.anomaliesEstimationPeriod &&
                maxAnomaliesPerEstimationPeriodPercentage == configuration.maxAnomaliesPerEstimationPeriodPercentage &&
                maxAnomaliestPerType == configuration.maxAnomaliestPerType && anomalyAutoLabeling == configuration.anomalyAutoLabeling &&
                Objects.equals(behaviorTypeLabelStrategy, configuration.behaviorTypeLabelStrategy);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(baseRepresentation, baseField, fast, sensitivityAutoAdjustment,
                initialSensitivity, sensitivityIncrement, maxSensitivity, initialLearningPeriod,
                initialAdjustmentLearningPeriod, anomaliesEstimationPeriod,
                maxAnomaliesPerEstimationPeriodPercentage, maxAnomaliestPerType, anomalyAutoLabeling, behaviorTypeLabelStrategy);
    }
}
