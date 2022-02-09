/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.values.IAnomalyValue;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link AnomalyBuilder} is a measurement data for anomaly fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class AnomalyBuilder implements IFieldValueBuilder, IAnomalyValue {
    private static final int CACHE_SIZE = Memory.getShallowSize(AnomalyBuilder.class);
    private float anomalyScore;
    private int behaviorType;
    private boolean anomaly;
    private boolean primaryAnomaly;
    private int id;

    public AnomalyBuilder() {
    }

    public AnomalyBuilder(float anomalyScore, int behaviorType, boolean anomaly, boolean primaryAnomaly, int id) {
        this.anomalyScore = anomalyScore;
        this.behaviorType = behaviorType;
        this.anomaly = anomaly;
        this.primaryAnomaly = primaryAnomaly;
        this.id = id;
    }

    @Override
    public float getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(float anomalyScore) {
        this.anomalyScore = anomalyScore;
    }

    @Override
    public int getBehaviorType() {
        return behaviorType;
    }

    public void setBehaviorType(int behaviorType) {
        this.behaviorType = behaviorType;
    }

    @Override
    public boolean isAnomaly() {
        return anomaly;
    }

    public void setAnomaly(boolean value) {
        this.anomaly = value;
    }

    @Override
    public boolean isPrimaryAnomaly() {
        return primaryAnomaly;
    }

    public void setPrimaryAnomaly(boolean value) {
        this.primaryAnomaly = value;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int value) {
        this.id = value;
    }

    @Override
    public IJsonCollection toJson() {
        return toValue().toJson();
    }

    @Override
    public void set(IFieldValue value) {
        Assert.notNull(value);

        IAnomalyValue anomalyValue = (IAnomalyValue) value;

        anomalyScore = anomalyValue.getAnomalyScore();
        behaviorType = anomalyValue.getBehaviorType();
        anomaly = anomalyValue.isAnomaly();
        primaryAnomaly = anomalyValue.isPrimaryAnomaly();
        id = anomalyValue.getId();
    }

    @Override
    public IFieldValue toValue() {
        return new AnomalyValue(anomalyScore, behaviorType, anomaly, primaryAnomaly, id);
    }

    @Override
    public void clear() {
        this.anomalyScore = 0;
        this.behaviorType = 0;
        this.anomaly = false;
        this.primaryAnomaly = false;
        this.id = 0;
    }

    @Override
    public void normalizeEnd(long count) {
    }

    @Override
    public void normalizeDerived(FieldValueSchemaConfiguration fieldSchemaConfiguration, long sum) {
    }

    @Override
    public int getCacheSize() {
        return CACHE_SIZE;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AnomalyBuilder))
            return false;

        AnomalyBuilder data = (AnomalyBuilder) o;
        return anomalyScore == data.anomalyScore && behaviorType == data.behaviorType && anomaly == data.anomaly &&
                primaryAnomaly == data.primaryAnomaly && id == data.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(anomalyScore, behaviorType, anomaly, primaryAnomaly, id);
    }

    @Override
    public String toString() {
        return toValue().toString();
    }
}
