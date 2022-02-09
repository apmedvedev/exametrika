/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.server.values;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.metrics.jvm.server.config.model.AppErrorsRepresentationSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.metrics.jvm.server.values.AppErrorsAccessor.Type;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IMetricAccessor;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;


/**
 * The {@link AppErrorsAccessorFactory} is a app errors metric accessor factory.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AppErrorsAccessorFactory implements IMetricAccessorFactory {
    private final AppErrorsRepresentationSchemaConfiguration configuration;
    private final ComponentValueSchemaConfiguration componentSchema;
    private final int metricIndex;
    private IComponentAccessorFactory componentAccessorFactory;

    public AppErrorsAccessorFactory(AppErrorsRepresentationSchemaConfiguration configuration,
                                    ComponentValueSchemaConfiguration componentSchema, int metricIndex) {
        Assert.notNull(configuration);
        Assert.notNull(componentSchema);

        this.configuration = configuration;
        this.componentSchema = componentSchema;
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
        return new AppErrorsAccessor(getType(fieldName), new AppErrorsComputer(configuration, componentAccessorFactory,
                componentSchema.getMetrics().get(metricIndex).getName(), metricIndex));
    }

    private Type getType(String fieldName) {
        if (fieldName.isEmpty() || fieldName.equals("value"))
            return Type.VALUE;
        else if (fieldName.equals("thresholds"))
            return Type.THRESHOLDS;
        else
            return Assert.error();
    }
}
