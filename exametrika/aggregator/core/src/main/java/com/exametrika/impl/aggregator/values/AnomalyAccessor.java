/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.values.IAnomalyValue;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldAccessor;


/**
 * The {@link AnomalyAccessor} is an implementation of {@link IFieldAccessor} for anomaly fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class AnomalyAccessor implements IFieldAccessor {
    protected final Type type;
    protected final AnomalyComputer computer;

    public enum Type {
        ANOMALY_SCORE,
        ANOMALY_LEVEL,
        ANOMALY,
        PRIMARY_ANOMALY,
        BEHAVIOR_TYPE,
        BEHAVIOR_TYPE_METADATA,
        BEHAVIOR_TYPE_LABELS,
        PREDICTIONS
    }

    public AnomalyAccessor(Type type, AnomalyComputer computer) {
        Assert.notNull(type);
        Assert.notNull(computer);

        this.type = type;
        this.computer = computer;
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue v, IComputeContext context) {
        IAnomalyValue value = (IAnomalyValue) v;
        if (value == null)
            return null;

        switch (type) {
            case ANOMALY_SCORE:
                return value.getAnomalyScore();
            case ANOMALY_LEVEL:
                return computer.getAnomalyLevel(value);
            case ANOMALY:
                return value.isAnomaly();
            case PRIMARY_ANOMALY:
                return value.isPrimaryAnomaly();
            case BEHAVIOR_TYPE:
                return computer.getBehaviorType(value, context);
            case BEHAVIOR_TYPE_METADATA:
                return computer.getBehaviorTypeMetadata(value, context);
            case BEHAVIOR_TYPE_LABELS:
                return computer.getBehaviorTypeLabels(value, context);
            default:
                return Assert.error();
        }
    }
}
