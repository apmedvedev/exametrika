/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.profiler.probes.SimpleRequestMappingStrategy;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequestMappingStrategy;
import com.exametrika.spi.profiler.config.RequestMappingStrategyConfiguration;


/**
 * The {@link SimpleRequestMappingStrategyConfiguration} is a simple request mapping strategy configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class SimpleRequestMappingStrategyConfiguration extends RequestMappingStrategyConfiguration {
    private final String nameExpression;
    private final String metadataExpression;
    private final String parametersExpression;
    private final String requestFilter;

    public SimpleRequestMappingStrategyConfiguration(String nameExpression, String metadataExpression,
                                                     String parametersExpression, String requestFilter) {
        Assert.notNull(nameExpression);
        Assert.notNull(metadataExpression);
        Assert.notNull(parametersExpression);

        this.nameExpression = nameExpression;
        this.metadataExpression = metadataExpression;
        this.parametersExpression = parametersExpression;
        this.requestFilter = requestFilter;
    }

    public final String getNameExpression() {
        return nameExpression;
    }

    public final String getMetadataExpression() {
        return metadataExpression;
    }

    public final String getParametersExpression() {
        return parametersExpression;
    }

    public final String getRequestFilter() {
        return requestFilter;
    }

    @Override
    public IRequestMappingStrategy createStrategy(IProbeContext context) {
        return new SimpleRequestMappingStrategy(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SimpleRequestMappingStrategyConfiguration))
            return false;

        SimpleRequestMappingStrategyConfiguration configuration = (SimpleRequestMappingStrategyConfiguration) o;
        return nameExpression.equals(configuration.nameExpression) && metadataExpression.equals(configuration.metadataExpression) &&
                parametersExpression.equals(configuration.parametersExpression) && Objects.equals(requestFilter, configuration.requestFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nameExpression, metadataExpression, parametersExpression, requestFilter);
    }

    @Override
    public String toString() {
        return nameExpression;
    }
}
