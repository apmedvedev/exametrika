/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.server.config.model;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ComponentRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ObjectRepresentationSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.jvm.server.values.AppErrorsAccessorFactory;
import com.exametrika.impl.metrics.jvm.server.values.AppErrorsComputer;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.IMetricComputer;


/**
 * The {@link AppErrorsRepresentationSchemaConfiguration} is a configuration for application errors representation schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AppErrorsRepresentationSchemaConfiguration extends ObjectRepresentationSchemaConfiguration {
    private final Type type;
    private double warningThreshold;
    private double errorThreshold;

    public enum Type {
        APP_REQUEST_ERRORS,
        APP_STALLS_ERRORS
    }

    public AppErrorsRepresentationSchemaConfiguration(String name, Type type, double warningThreshold, double errorThreshold) {
        super(name);

        Assert.notNull(type);
        Assert.isTrue(warningThreshold >= 0);
        Assert.isTrue(errorThreshold >= 0);
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
        return new AppErrorsComputer(this, componentAccessorFactory, componentSchema.getMetrics().get(metricIndex).getName(),
                metricIndex);
    }

    @Override
    public IMetricAccessorFactory createAccessorFactory(ComponentValueSchemaConfiguration componentSchema,
                                                        ComponentRepresentationSchemaConfiguration componentConfiguration, int metricIndex) {
        return new AppErrorsAccessorFactory(this, componentSchema, metricIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AppErrorsRepresentationSchemaConfiguration))
            return false;

        AppErrorsRepresentationSchemaConfiguration configuration = (AppErrorsRepresentationSchemaConfiguration) o;
        return type.equals(configuration.type) && warningThreshold == configuration.warningThreshold &&
                errorThreshold == configuration.errorThreshold;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, warningThreshold, errorThreshold);
    }
}
