/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.host.server.config.model;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ComponentRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ObjectRepresentationSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.host.server.values.HostWorkloadAccessorFactory;
import com.exametrika.impl.metrics.host.server.values.HostWorkloadComputer;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.IMetricComputer;


/**
 * The {@link HostWorkloadRepresentationSchemaConfiguration} is a configuration for host workload representation schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HostWorkloadRepresentationSchemaConfiguration extends ObjectRepresentationSchemaConfiguration {
    private final Type type;
    private double warningThreshold;
    private double errorThreshold;

    public enum Type {
        HOST_CPU_WORKLOAD,
        HOST_MEMORY_WORKLOAD,
        HOST_DISK_WORKLOAD,
        HOST_NET_RECEIVE_WORKLOAD,
        HOST_NET_SEND_WORKLOAD,
        HOST_SWAP_WORKLOAD,
    }

    public HostWorkloadRepresentationSchemaConfiguration(String name, Type type, double warningThreshold, double errorThreshold) {
        super(name);

        Assert.notNull(type);
        Assert.isTrue(warningThreshold >= 0 && warningThreshold <= 100);
        Assert.isTrue(errorThreshold >= 0 && errorThreshold <= 100);
        Assert.isTrue(warningThreshold <= errorThreshold);

        this.warningThreshold = warningThreshold;
        this.errorThreshold = errorThreshold;
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public double getWarningThreshold() {
        return warningThreshold;
    }

    public double getErrorThreshold() {
        return errorThreshold;
    }

    @Override
    public IMetricComputer createComputer(ComponentValueSchemaConfiguration componentSchema,
                                          ComponentRepresentationSchemaConfiguration componentConfiguration, IComponentAccessorFactory componentAccessorFactory,
                                          int metricIndex) {
        return new HostWorkloadComputer(this, componentAccessorFactory, componentSchema.getMetrics().get(metricIndex).getName(),
                componentConfiguration.getName(), metricIndex);
    }

    @Override
    public IMetricAccessorFactory createAccessorFactory(ComponentValueSchemaConfiguration componentSchema,
                                                        ComponentRepresentationSchemaConfiguration componentConfiguration, int metricIndex) {
        return new HostWorkloadAccessorFactory(this, componentSchema, componentConfiguration, metricIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HostWorkloadRepresentationSchemaConfiguration))
            return false;

        HostWorkloadRepresentationSchemaConfiguration configuration = (HostWorkloadRepresentationSchemaConfiguration) o;
        return type.equals(configuration.type) && warningThreshold == configuration.warningThreshold &&
                errorThreshold == configuration.errorThreshold;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, warningThreshold, errorThreshold);
    }
}
