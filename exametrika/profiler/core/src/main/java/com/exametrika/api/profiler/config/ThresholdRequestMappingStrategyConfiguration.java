/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import com.exametrika.common.utils.Objects;
import com.exametrika.impl.profiler.probes.ThresholdRequestMappingStrategy;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequestMappingStrategy;


/**
 * The {@link ThresholdRequestMappingStrategyConfiguration} is a threshold request mapping strategy configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ThresholdRequestMappingStrategyConfiguration extends SimpleRequestMappingStrategyConfiguration {
    private final long estimationPeriod;
    private final long measurementPeriod;
    private final long threshold;
    private final int maxRequestCount;
    private final String beginValueExpression;
    private final String endValueExpression;
    private final double requestPercentage;

    public ThresholdRequestMappingStrategyConfiguration(String nameExpression, String metadataExpression,
                                                        String parametersExpression, String requestFilter, String beginValueExpression, String endValueExpression, long threshold,
                                                        long estimationPeriod, long measurementPeriod, int maxRequestCount, double requestPercentage) {
        super(nameExpression, metadataExpression, parametersExpression, requestFilter);

        this.beginValueExpression = beginValueExpression;
        this.endValueExpression = endValueExpression;
        this.threshold = threshold;
        this.estimationPeriod = estimationPeriod;
        this.measurementPeriod = measurementPeriod;
        this.maxRequestCount = maxRequestCount;
        this.requestPercentage = requestPercentage;
    }

    public String getBeginValueExpression() {
        return beginValueExpression;
    }

    public String getEndValueExpression() {
        return endValueExpression;
    }

    public long getThreshold() {
        return threshold;
    }

    public long getEstimationPeriod() {
        return estimationPeriod;
    }

    public long getMeasurementPeriod() {
        return measurementPeriod;
    }

    public int getMaxRequestCount() {
        return maxRequestCount;
    }

    public double getRequestPercentage() {
        return requestPercentage;
    }

    @Override
    public IRequestMappingStrategy createStrategy(IProbeContext context) {
        return new ThresholdRequestMappingStrategy(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ThresholdRequestMappingStrategyConfiguration))
            return false;

        ThresholdRequestMappingStrategyConfiguration configuration = (ThresholdRequestMappingStrategyConfiguration) o;
        return super.equals(configuration) && Objects.equals(beginValueExpression, configuration.beginValueExpression) &&
                Objects.equals(endValueExpression, configuration.endValueExpression) &&
                threshold == configuration.threshold &&
                estimationPeriod == configuration.estimationPeriod && measurementPeriod == configuration.measurementPeriod &&
                maxRequestCount == configuration.maxRequestCount && requestPercentage == configuration.requestPercentage;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(beginValueExpression, endValueExpression,
                threshold, estimationPeriod, measurementPeriod, maxRequestCount, requestPercentage);
    }
}
