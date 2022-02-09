/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.server.config.model;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ComponentRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ObjectRepresentationSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.jvm.server.values.AppWorkloadAccessorFactory;
import com.exametrika.impl.metrics.jvm.server.values.AppWorkloadComputer;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.IMetricComputer;


/**
 * The {@link AppWorkloadRepresentationSchemaConfiguration} is a configuration for application workload representation schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AppWorkloadRepresentationSchemaConfiguration extends ObjectRepresentationSchemaConfiguration {
    private final Type type;
    private double warningThreshold;
    private double errorThreshold;

    public enum Type {
        APP_LATENCY_WORKLOAD,
        APP_THROUGHPUT_WORKLOAD,
    }

    public AppWorkloadRepresentationSchemaConfiguration(String name, Type type, double warningThreshold, double errorThreshold) {
        super(name);

        Assert.notNull(type);

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
        return new AppWorkloadComputer(this, componentAccessorFactory, componentSchema.getMetrics().get(metricIndex).getName(),
                metricIndex);
    }

    @Override
    public IMetricAccessorFactory createAccessorFactory(ComponentValueSchemaConfiguration componentSchema,
                                                        ComponentRepresentationSchemaConfiguration componentConfiguration, int metricIndex) {
        return new AppWorkloadAccessorFactory(this, componentSchema, metricIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AppWorkloadRepresentationSchemaConfiguration))
            return false;

        AppWorkloadRepresentationSchemaConfiguration configuration = (AppWorkloadRepresentationSchemaConfiguration) o;
        return type.equals(configuration.type) && warningThreshold == configuration.warningThreshold &&
                errorThreshold == configuration.errorThreshold;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, warningThreshold, errorThreshold);
    }
}
