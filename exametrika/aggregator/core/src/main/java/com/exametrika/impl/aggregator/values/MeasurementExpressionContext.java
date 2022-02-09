/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IMeasurementExpressionContext;

/**
 * The {@link MeasurementExpressionContext} is measurement expression context.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class MeasurementExpressionContext implements IMeasurementExpressionContext {
    private IComponentAccessorFactory componentAccessorFactory;
    private IComponentValue value;
    private IComputeContext context;

    @Override
    public boolean hasMetric(String key) {
        return componentAccessorFactory.hasMetric(key);
    }

    @Override
    public Object metric(String key) {
        if (!componentAccessorFactory.hasMetric(key))
            return null;

        IComponentAccessor accessor = componentAccessorFactory.createAccessor(null, null, key);
        return accessor.get(value, context);
    }

    public void setValue(IComponentValue value) {
        this.value = value;
    }

    public void setComputeContext(IComputeContext context) {
        this.context = context;
    }

    public void setComponentAccessorFactory(IComponentAccessorFactory componentAccessorFactory) {
        this.componentAccessorFactory = componentAccessorFactory;
    }

    public void clear() {
        value = null;
        context = null;
        componentAccessorFactory = null;
    }
}