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


/**
 * The {@link PercentageAccessor} is an implementation of {@link IFieldAccessor} for percentage fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class PercentageAccessor implements IFieldAccessor {
    private final PercentageComputer computer;

    public PercentageAccessor(PercentageComputer computer) {
        Assert.notNull(computer);

        this.computer = computer;
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value, IComputeContext context) {
        return computer.getPercentage(componentValue, metricValue, context);
    }
}
