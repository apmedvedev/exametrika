/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldAccessor;


/**
 * The {@link ForecastAccessor} is an implementation of {@link IFieldAccessor} for forecast fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ForecastAccessor extends AnomalyAccessor {
    public ForecastAccessor(Type type, ForecastComputer computer) {
        super(type, computer);
    }

    @Override
    public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue v, IComputeContext context) {
        switch (type) {
            case PREDICTIONS:
                return ((ForecastComputer) computer).getPredictions(context);
            default:
                return super.get(componentValue, metricValue, v, context);
        }
    }
}
