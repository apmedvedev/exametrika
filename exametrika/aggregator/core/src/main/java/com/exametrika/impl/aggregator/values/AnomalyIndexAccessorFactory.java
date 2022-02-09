/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.values.AnomalyIndexAccessor.Type;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IMetricAccessor;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;


/**
 * The {@link AnomalyIndexAccessorFactory} is a object metric accessor factory.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AnomalyIndexAccessorFactory implements IMetricAccessorFactory {
    private final ComponentValueSchemaConfiguration componentConfiguration;
    private final int metricIndex;
    private IComponentAccessorFactory componentAccessorFactory;

    public AnomalyIndexAccessorFactory(ComponentValueSchemaConfiguration componentConfiguration, int metricIndex) {
        Assert.notNull(componentConfiguration);

        this.componentConfiguration = componentConfiguration;
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
    }

    @Override
    public IMetricAccessor createAccessor(String navigationType, String navigationArgs, String fieldName) {
        return new AnomalyIndexAccessor(getType(fieldName), new AnomalyIndexComputer(componentConfiguration, metricIndex));
    }

    private Type getType(String fieldName) {
        if (fieldName.isEmpty() || fieldName.equals("state"))
            return Type.STATE;
        else if (fieldName.equals("causes"))
            return Type.CAUSES;
        else
            return Assert.error();
    }
}
