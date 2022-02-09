/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.server.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IMetricAccessor;


/**
 * The {@link AppWorkloadAccessor} is an implementation of {@link IFieldAccessor} for application workload metric.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class AppWorkloadAccessor implements IMetricAccessor {
    public enum Type {
        VALUE,
        THRESHOLDS
    }

    private final Type type;
    private final AppWorkloadComputer computer;

    public AppWorkloadAccessor(Type type, AppWorkloadComputer computer) {
        Assert.notNull(type);
        Assert.notNull(computer);

        this.type = type;
        this.computer = computer;
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IComputeContext context) {
        return computer.getValue(metricValue, type == Type.THRESHOLDS);
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value,
                      IComputeContext context) {
        return get(componentValue, metricValue, context);
    }
}
