/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.values.IAnomalyValue;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Objects;


/**
 * The {@link AnomalyValue} is a measurement data for anomaly fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AnomalyValue implements IAnomalyValue {
    private final float anomalyScore;
    private final int behaviorType;
    private final boolean anomaly;
    private final boolean primaryAnomaly;
    private final int id;

    public AnomalyValue(float anomalyScore, int behaviorType, boolean anomaly, boolean primaryAnomaly, int id) {
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

    @Override
    public int getBehaviorType() {
        return behaviorType;
    }

    @Override
    public boolean isAnomaly() {
        return anomaly;
    }

    @Override
    public boolean isPrimaryAnomaly() {
        return primaryAnomaly;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public JsonObject toJson() {
        JsonObjectBuilder fields = new JsonObjectBuilder();

        fields.put("instanceOf", "anomaly");
        fields.put("anomalyScore", anomalyScore);
        fields.put("behaviorType", behaviorType);
        if (anomaly)
            fields.put("anomaly", true);
        if (primaryAnomaly)
            fields.put("primary", true);

        return fields.toJson();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AnomalyValue))
            return false;

        AnomalyValue data = (AnomalyValue) o;
        return anomalyScore == data.anomalyScore && behaviorType == data.behaviorType && anomaly == data.anomaly &&
                primaryAnomaly == data.primaryAnomaly && id == data.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(anomalyScore, behaviorType, anomaly, primaryAnomaly, id);
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
