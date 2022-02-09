/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.server.values;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.metrics.jvm.server.config.model.JvmWorkloadRepresentationSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.metrics.jvm.server.values.JvmWorkloadAccessor.Type;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IMetricAccessor;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;


/**
 * The {@link JvmWorkloadAccessorFactory} is a jvm workload metric accessor factory.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JvmWorkloadAccessorFactory implements IMetricAccessorFactory {
    private final JvmWorkloadRepresentationSchemaConfiguration configuration;
    private final ComponentValueSchemaConfiguration componentSchema;
    private final int metricIndex;
    private IComponentAccessorFactory componentAccessorFactory;

    public JvmWorkloadAccessorFactory(JvmWorkloadRepresentationSchemaConfiguration configuration,
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
        return new JvmWorkloadAccessor(getType(fieldName), new JvmWorkloadComputer(configuration, componentAccessorFactory,
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
