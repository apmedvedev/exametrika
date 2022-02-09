/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IMetricAccessor;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;


/**
 * The {@link ExpressionIndexAccessorFactory} is a expression index metric accessor factory.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionIndexAccessorFactory implements IMetricAccessorFactory {
    private final boolean stored;
    private final int metricIndex;
    private final String expression;
    private IComponentAccessorFactory componentAccessorFactory;

    public ExpressionIndexAccessorFactory(boolean stored, int metricIndex, String expression) {
        this.stored = stored;
        this.metricIndex = metricIndex;
        this.expression = expression;
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
    }

    @Override
    public IMetricAccessor createAccessor(String navigationType, String navigationArgs, String fieldName) {
        return new ExpressionIndexAccessor(stored, stored ? null : new ExpressionIndexComputer(stored, expression, componentAccessorFactory));
    }
}
