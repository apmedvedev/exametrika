/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IStackValue;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IMetricAccessor;


/**
 * The {@link StackStoredAccessor} is an accessor of fields in stack metric type.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackStoredAccessor implements IMetricAccessor {
    private final boolean inherent;
    private final int fieldIndex;
    private final IFieldAccessor childAccessor;
    private final boolean opposite;

    public StackStoredAccessor(boolean inherent, int fieldIndex, IFieldAccessor childAccessor, boolean opposite) {
        Assert.notNull(childAccessor);

        this.inherent = inherent;
        this.fieldIndex = fieldIndex;
        this.childAccessor = childAccessor;
        this.opposite = opposite;
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
        IStackValue metricValue = (IStackValue) value;
        if (metricValue == null)
            return null;

        if (opposite) {
            if (inherent && context.isInherent())
                return null;
            if (!inherent && context.isTotal())
                return null;
        }

        if (inherent)
            return childAccessor.get(componentValue, metricValue, metricValue.getInherentFields().get(fieldIndex), context);
        else
            return childAccessor.get(componentValue, metricValue, metricValue.getTotalFields().get(fieldIndex), context);
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value,
                      IComputeContext context) {
        return get(componentValue, metricValue, context);
    }
}
