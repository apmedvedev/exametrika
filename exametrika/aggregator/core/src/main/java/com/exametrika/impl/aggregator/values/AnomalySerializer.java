/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;


import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.values.IAnomalyValue;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;


/**
 * The {@link AnomalySerializer} is an implementation of {@link IFieldValueSerializer} for {@link AnomalyValue}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AnomalySerializer implements IFieldValueSerializer {
    private final boolean builder;

    public AnomalySerializer(boolean builder) {
        this.builder = builder;
    }

    @Override
    public void serialize(IDataSerialization serialization, IFieldValue value) {
        IAnomalyValue anomalyValue = (IAnomalyValue) value;
        serialization.writeFloat(anomalyValue.getAnomalyScore());
        serialization.writeInt(anomalyValue.getBehaviorType());
        serialization.writeBoolean(anomalyValue.isAnomaly());
        serialization.writeBoolean(anomalyValue.isPrimaryAnomaly());
        serialization.writeInt(anomalyValue.getId());
    }

    @Override
    public IFieldValue deserialize(IDataDeserialization deserialization) {
        float anomalyScore = deserialization.readFloat();
        int behaviorType = deserialization.readInt();
        boolean anomaly = deserialization.readBoolean();
        boolean primaryAnomaly = deserialization.readBoolean();
        int id = deserialization.readInt();

        if (builder)
            return new AnomalyBuilder(anomalyScore, behaviorType, anomaly, primaryAnomaly, id);
        else
            return new AnomalyValue(anomalyScore, behaviorType, anomaly, primaryAnomaly, id);
    }
}
