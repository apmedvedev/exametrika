/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IMetricAccessor;


/**
 * The {@link AnomalyIndexAccessor} is an implementation of {@link IFieldAccessor} for anomaly index.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class AnomalyIndexAccessor implements IMetricAccessor {
    public enum Type {
        STATE,
        CAUSES
    }

    private final Type type;
    private final AnomalyIndexComputer computer;

    public AnomalyIndexAccessor(Type type, AnomalyIndexComputer computer) {
        Assert.notNull(type);
        Assert.notNull(computer);

        this.type = type;
        this.computer = computer;
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IComputeContext context) {
        return computer.getValue(metricValue, type == Type.CAUSES);
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value,
                      IComputeContext context) {
        return get(componentValue, metricValue, context);
    }
}
