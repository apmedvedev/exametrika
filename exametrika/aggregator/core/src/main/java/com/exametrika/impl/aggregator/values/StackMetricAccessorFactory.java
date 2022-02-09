/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IMetricAccessor;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;


/**
 * The {@link StackMetricAccessorFactory} is a stack metric accessor factory.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackMetricAccessorFactory implements IMetricAccessorFactory {
    private final IMetricAccessorFactory inherentFieldAccessorFactory;
    private final IMetricAccessorFactory totalFieldAccessorFactory;
    private final int metricIndex;
    private IComponentAccessorFactory componentAccessorFactory;

    public StackMetricAccessorFactory(IMetricAccessorFactory inherentFieldAccessorFactory,
                                      IMetricAccessorFactory totalFieldAccessorFactory, int metricIndex) {
        Assert.notNull(inherentFieldAccessorFactory);
        Assert.notNull(totalFieldAccessorFactory);

        this.inherentFieldAccessorFactory = inherentFieldAccessorFactory;
        this.totalFieldAccessorFactory = totalFieldAccessorFactory;
        this.metricIndex = metricIndex;
    }

    @Override
    public int getMetricIndex() {
        return metricIndex;
    }

    @Override
    public IComponentAccessorFactory getComponentAccessorFactory() {
        return componentAccessorFactory;
    }

    @Override
    public void setComponentAccessorFactory(IComponentAccessorFactory componentAccessorFactory) {
        Assert.notNull(componentAccessorFactory);
        Assert.checkState(this.componentAccessorFactory == null);

        this.componentAccessorFactory = componentAccessorFactory;

        inherentFieldAccessorFactory.setComponentAccessorFactory(componentAccessorFactory);
        totalFieldAccessorFactory.setComponentAccessorFactory(componentAccessorFactory);
    }

    @Override
    public IMetricAccessor createAccessor(String navigationType, String navigationArgs, String fieldName) {
        int pos = fieldName.indexOf('.');
        Assert.isTrue(pos != -1);

        String metricName = fieldName.substring(0, pos);
        fieldName = fieldName.substring(pos + 1);

        if (metricName.equals("inherent"))
            return inherentFieldAccessorFactory.createAccessor(navigationType, navigationArgs, fieldName);
        else if (metricName.equals("total"))
            return totalFieldAccessorFactory.createAccessor(navigationType, navigationArgs, fieldName);
        else
            return Assert.error();
    }
}
