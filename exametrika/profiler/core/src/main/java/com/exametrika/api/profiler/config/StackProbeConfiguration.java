/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.StackValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.profiler.probes.StackProbe;
import com.exametrika.spi.aggregator.common.meters.config.FieldConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.GaugeConfiguration;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ProbeConfiguration;
import com.exametrika.spi.profiler.config.StackCounterConfiguration;


/**
 * The {@link StackProbeConfiguration} is a configuration of stack probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackProbeConfiguration extends ProbeConfiguration {
    private final List<FieldConfiguration> fields;
    private final List<StackCounterConfiguration> stackCounters;
    private final GaugeConfiguration concurrencyLevel;
    private final long minEstimationPeriod;
    private final long maxEstimationPeriod;
    private final int minHotspotCount;
    private final int maxHotspotCount;
    private final int hotspotStep;
    private final double hotspotCoverage;
    private final double tolerableOverhead;
    private final long ultraFastMethodThreshold;
    private final int idleRetentionCount;
    private final int extractionDelayCount;
    private final long preaggregationPeriod;
    private final CombineType combineType;
    private final String stackMeasurementStrategy;

    public enum CombineType {
        STACK,
        TRANSACTION,
        NODE,
        ALL
    }

    public StackProbeConfiguration(String name, String scopeType, long extractionPeriod, String measurementStrategy,
                                   long warmupDelay, List<? extends FieldConfiguration> fields, List<? extends StackCounterConfiguration> stackCounters,
                                   GaugeConfiguration concurrencyLevel,
                                   long minEstimationPeriod, long maxEstimationPeriod, int minHotspotCount, int maxHotspotCount,
                                   int hotspotStep, double hotspotCoverage, double tolerableOverhead, long ultraFastMethodThreshold, int idleRetentionCount,
                                   int extractionDelayCount, long preaggregationPeriod, CombineType combineType, String stackMeasurementStrategy) {
        super(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay);

        Assert.notNull(fields);
        Assert.notNull(stackCounters);
        Assert.notNull(concurrencyLevel);
        Assert.isTrue(!fields.isEmpty());
        Assert.isTrue(minHotspotCount > 0 && maxHotspotCount > 0 && minHotspotCount <= maxHotspotCount && hotspotStep > 0);
        Assert.isTrue(hotspotCoverage >= 0 && hotspotCoverage <= 100);
        Assert.isTrue(tolerableOverhead >= 0.0001);
        Assert.notNull(combineType);

        this.fields = Immutables.wrap(fields);
        this.stackCounters = Immutables.wrap(stackCounters);
        this.concurrencyLevel = concurrencyLevel;
        this.minEstimationPeriod = minEstimationPeriod;
        this.maxEstimationPeriod = maxEstimationPeriod;
        this.minHotspotCount = minHotspotCount;
        this.maxHotspotCount = maxHotspotCount;
        this.hotspotStep = hotspotStep;
        this.hotspotCoverage = hotspotCoverage;
        this.tolerableOverhead = tolerableOverhead;
        this.ultraFastMethodThreshold = ultraFastMethodThreshold;
        this.idleRetentionCount = idleRetentionCount;
        this.extractionDelayCount = extractionDelayCount;
        this.preaggregationPeriod = preaggregationPeriod;
        this.combineType = combineType;
        this.stackMeasurementStrategy = stackMeasurementStrategy;
    }

    public List<FieldConfiguration> getFields() {
        return fields;
    }

    public List<StackCounterConfiguration> getStackCounters() {
        return stackCounters;
    }

    public GaugeConfiguration getConcurrencyLevel() {
        return concurrencyLevel;
    }

    public long getMinEstimationPeriod() {
        return minEstimationPeriod;
    }

    public long getMaxEstimationPeriod() {
        return maxEstimationPeriod;
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

    public double getTolerableOverhead() {
        return tolerableOverhead;
    }

    public double getUltraFastMethodThreshold() {
        return ultraFastMethodThreshold;
    }

    public int getIdleRetentionCount() {
        return idleRetentionCount;
    }

    public int getExtractionDelayCount() {
        return extractionDelayCount;
    }

    public long getPreaggregationPeriod() {
        return preaggregationPeriod;
    }

    public CombineType getCombineType() {
        return combineType;
    }

    public String getStackMeasurementStrategy() {
        return stackMeasurementStrategy;
    }

    @Override
    public String getComponentType() {
        return "app.stack";
    }

    public StackProbeConfiguration getCalibratorConfiguration() {
        return new StackProbeConfiguration("", "", 1, null, 0, fields, stackCounters, concurrencyLevel, 1, 1, 1, 1, 1, 0, 1, 400, 10, 3, 0,
                CombineType.STACK, null);
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new StackProbe(this, context);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        ComponentValueSchemaBuilder builder = ValueSchemas.component(getComponentType());

        for (MetricValueSchemaConfiguration metric : getStackMetrics())
            builder.metric(metric);

        if (concurrencyLevel.isEnabled())
            builder.metric(concurrencyLevel.getSchema("app.concurrency"));

        components.add(builder.toConfiguration());

        components.add(ValueSchemas.component("app.stack.root")
                .metric(getMainStackMetric())
                .toConfiguration());
    }

    public MetricValueSchemaConfiguration getMainStackMetric() {
        List<FieldValueSchemaConfiguration> fields = new ArrayList<FieldValueSchemaConfiguration>(this.fields.size());
        for (FieldConfiguration field : this.fields)
            fields.add(field.getSchema());

        return new StackValueSchemaConfiguration("app.cpu.time", fields);
    }

    public List<MetricValueSchemaConfiguration> getStackMetrics() {
        List<MetricValueSchemaConfiguration> metrics = new ArrayList<MetricValueSchemaConfiguration>();

        metrics.add(getMainStackMetric());

        for (StackCounterConfiguration stackCounter : stackCounters) {
            if (stackCounter.isEnabled())
                metrics.add(stackCounter.getSchema());
        }

        return metrics;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackProbeConfiguration))
            return false;

        StackProbeConfiguration configuration = (StackProbeConfiguration) o;
        return super.equals(configuration) &&
                fields.equals(configuration.fields) &&
                stackCounters.equals(configuration.stackCounters) &&
                concurrencyLevel.equals(configuration.concurrencyLevel) &&
                minEstimationPeriod == configuration.minEstimationPeriod &&
                maxEstimationPeriod == configuration.maxEstimationPeriod &&
                minHotspotCount == configuration.minHotspotCount && maxHotspotCount == configuration.maxHotspotCount &&
                hotspotStep == configuration.hotspotStep && hotspotCoverage == configuration.hotspotCoverage &&
                tolerableOverhead == configuration.tolerableOverhead && ultraFastMethodThreshold == configuration.ultraFastMethodThreshold &&
                idleRetentionCount == configuration.idleRetentionCount && extractionDelayCount == configuration.extractionDelayCount &&
                preaggregationPeriod == configuration.preaggregationPeriod && combineType == configuration.combineType &&
                Objects.equals(stackMeasurementStrategy, configuration.stackMeasurementStrategy);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(fields, stackCounters, concurrencyLevel, minEstimationPeriod,
                maxEstimationPeriod, minHotspotCount, maxHotspotCount, hotspotStep, hotspotCoverage, tolerableOverhead,
                ultraFastMethodThreshold, idleRetentionCount, extractionDelayCount, preaggregationPeriod, combineType,
                stackMeasurementStrategy);
    }
}
