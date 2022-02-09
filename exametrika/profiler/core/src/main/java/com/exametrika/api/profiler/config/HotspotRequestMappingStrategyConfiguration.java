/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.profiler.probes.HotspotRequestMappingStrategy;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequestMappingStrategy;
import com.exametrika.spi.profiler.config.RequestGroupingStrategyConfiguration;


/**
 * The {@link HotspotRequestMappingStrategyConfiguration} is a hotspot request mapping strategy configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HotspotRequestMappingStrategyConfiguration extends SimpleRequestMappingStrategyConfiguration {
    private final long estimationPeriod;
    private final long measurementPeriod;
    private final int minHotspotCount;
    private final int maxHotspotCount;
    private final int hotspotStep;
    private final double hotspotCoverage;
    private final int maxRequestCount;
    private final String beginValueExpression;
    private final String endValueExpression;
    private final RequestGroupingStrategyConfiguration groupingStrategy;
    private final boolean perThreadStatistics;

    public HotspotRequestMappingStrategyConfiguration(String nameExpression, String metadataExpression,
                                                      String parametersExpression, String requestFilter, String beginValueExpression, String endValueExpression,
                                                      long estimationPeriod, long measurementPeriod, int minHotspotCount, int maxHotspotCount,
                                                      int hotspotStep, double hotspotCoverage, int maxRequestCount, RequestGroupingStrategyConfiguration groupingStrategy,
                                                      boolean perThreadStatistics) {
        super(nameExpression, metadataExpression, parametersExpression, requestFilter);

        Assert.isTrue(minHotspotCount > 0 && maxHotspotCount > 0 && minHotspotCount <= maxHotspotCount && hotspotStep > 0);
        Assert.isTrue(hotspotCoverage >= 0 && hotspotCoverage <= 100);
        Assert.notNull(groupingStrategy);

        this.beginValueExpression = beginValueExpression;
        this.endValueExpression = endValueExpression;
        this.estimationPeriod = estimationPeriod;
        this.measurementPeriod = measurementPeriod;
        this.minHotspotCount = minHotspotCount;
        this.maxHotspotCount = maxHotspotCount;
        this.hotspotStep = hotspotStep;
        this.hotspotCoverage = hotspotCoverage;
        this.maxRequestCount = maxRequestCount;
        this.groupingStrategy = groupingStrategy;
        this.perThreadStatistics = perThreadStatistics;
    }

    public String getBeginValueExpression() {
        return beginValueExpression;
    }

    public String getEndValueExpression() {
        return endValueExpression;
    }

    public long getEstimationPeriod() {
        return estimationPeriod;
    }

    public long getMeasurementPeriod() {
        return measurementPeriod;
    }

    public int getMinHotspotCount() {
        return minHotspotCount;
    }

    public int getMaxHotspotCount() {
        return maxHotspotCount;
    }

    public int getHotspotStep() {
        return hotspotStep;
    }

    public double getHotspotCoverage() {
        return hotspotCoverage;
    }

    public int getMaxRequestCount() {
        return maxRequestCount;
    }

    public RequestGroupingStrategyConfiguration getGroupingStrategy() {
        return groupingStrategy;
    }

    public boolean isPerThreadStatistics() {
        return perThreadStatistics;
    }

    @Override
    public IRequestMappingStrategy createStrategy(IProbeContext context) {
        return new HotspotRequestMappingStrategy(this, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HotspotRequestMappingStrategyConfiguration))
            return false;

        HotspotRequestMappingStrategyConfiguration configuration = (HotspotRequestMappingStrategyConfiguration) o;
        return super.equals(configuration) && Objects.equals(beginValueExpression, configuration.beginValueExpression) &&
                Objects.equals(endValueExpression, configuration.endValueExpression) &&
                estimationPeriod == configuration.estimationPeriod && measurementPeriod == configuration.measurementPeriod &&
                minHotspotCount == configuration.minHotspotCount && maxHotspotCount == configuration.maxHotspotCount &&
                hotspotStep == configuration.hotspotStep && hotspotCoverage == configuration.hotspotCoverage &&
                maxRequestCount == configuration.maxRequestCount && groupingStrategy.equals(configuration.groupingStrategy) &&
                perThreadStatistics == configuration.perThreadStatistics;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(beginValueExpression, endValueExpression, estimationPeriod,
                measurementPeriod, minHotspotCount, maxHotspotCount, hotspotStep, hotspotCoverage, maxRequestCount,
                groupingStrategy, perThreadStatistics);
    }
}
