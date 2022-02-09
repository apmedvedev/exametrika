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
 * The {@link NameAccessor} is an accessor of fields in field metric type.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NameAccessor implements IMetricAccessor {
    private final IFieldAccessor childAccessor;

    public NameAccessor(IFieldAccessor childAccessor) {
        Assert.notNull(childAccessor);

        this.childAccessor = childAccessor;
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
        INameValue metricValue = (INameValue) value;
        return childAccessor.get(componentValue, metricValue, null, context);
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value,
                      IComputeContext context) {
        return get(componentValue, metricValue, context);
    }
}
