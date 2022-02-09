/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IStandardValue;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;


/**
 * The {@link StandardComputer} is an implementation of {@link IFieldComputer} for standard fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class StandardComputer implements IFieldComputer {
    @Override
    public Object compute(IComponentValue componentValue, IMetricValue metricValue, IFieldValue v, IComputeContext context) {
        IStandardValue value = (IStandardValue) v;

        long count = value.getCount();
        if (count != 0) {
            JsonObjectBuilder fields = new JsonObjectBuilder();
            fields.put("count", count);
            fields.put("sum", value.getSum());
            fields.put("min", value.getMin());
            fields.put("max", value.getMax());
            fields.put("avg", (double) value.getSum() / count);
            return fields.toJson();
        } else
            return null;
    }

    @Override
    public void computeSecondary(IComponentValue componentValue, IMetricValue metricValue, IFieldValueBuilder value, IComputeContext context) {
    }
}
