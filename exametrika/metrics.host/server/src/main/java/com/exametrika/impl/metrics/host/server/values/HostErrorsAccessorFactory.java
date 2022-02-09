/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.server.values;

import com.exametrika.api.metrics.host.server.config.model.HostErrorsRepresentationSchemaConfiguration;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ComponentRepresentationSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.metrics.host.server.values.HostErrorsAccessor.Type;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IMetricAccessor;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;


/**
 * The {@link HostErrorsAccessorFactory} is a host errors metric accessor factory.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostErrorsAccessorFactory implements IMetricAccessorFactory {
    private final HostErrorsRepresentationSchemaConfiguration configuration;
    private final ComponentValueSchemaConfiguration componentSchema;
    private final ComponentRepresentationSchemaConfiguration componentConfiguration;
    private final int metricIndex;
    private IComponentAccessorFactory componentAccessorFactory;

    public HostErrorsAccessorFactory(HostErrorsRepresentationSchemaConfiguration configuration,
                                     ComponentValueSchemaConfiguration componentSchema,
                                     ComponentRepresentationSchemaConfiguration componentConfiguration, int metricIndex) {
        Assert.notNull(configuration);
        Assert.notNull(componentSchema);
        Assert.notNull(componentConfiguration);

        this.configuration = configuration;
        this.componentSchema = componentSchema;
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
        return new HostErrorsAccessor(getType(fieldName), new HostErrorsComputer(configuration, componentAccessorFactory,
                componentSchema.getMetrics().get(metricIndex).getName(), componentConfiguration.getName(), metricIndex));
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
