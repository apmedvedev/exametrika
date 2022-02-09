/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.INameValue;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IMetricAccessor;


/**
 * The {@link NameStoredAccessor} is an accessor of fields in field metric type.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NameStoredAccessor implements IMetricAccessor {
    private final int fieldIndex;
    private final IFieldAccessor childAccessor;

    public NameStoredAccessor(int fieldIndex, IFieldAccessor childAccessor) {
        Assert.notNull(childAccessor);

        this.fieldIndex = fieldIndex;
        this.childAccessor = childAccessor;
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
        INameValue metricValue = (INameValue) value;
        if (metricValue != null)
            return childAccessor.get(componentValue, metricValue, metricValue.getFields().get(fieldIndex), context);
        else
            return null;
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value,
                      IComputeContext context) {
        return get(componentValue, metricValue, context);
    }
}
