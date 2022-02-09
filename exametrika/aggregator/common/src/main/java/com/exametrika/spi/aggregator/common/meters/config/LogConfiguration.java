/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.exametrika.api.aggregator.common.meters.config.LogMeterConfiguration;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ObjectValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;


/**
 * The {@link LogConfiguration} is a configuration of log.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class LogConfiguration extends MeterConfiguration {
    private final LogFilterConfiguration filter;
    private final List<LogMeterConfiguration> meters;
    private final LogFilterConfiguration postFilter;
    private final LogProviderConfiguration transformer;
    private final int maxStackTraceDepth;
    private final int maxMessageSize;
    private final int maxRate;
    private final int maxStackTraceRate;
    private final int maxBundleSize;

    public LogConfiguration(boolean enabled, LogFilterConfiguration filter, List<LogMeterConfiguration> meters,
                            LogFilterConfiguration postFilter, LogProviderConfiguration transformer, int maxStackTraceDepth, int maxMessageSize,
                            int maxRate, int maxStackTraceRate, int maxBundleSize) {
        super(enabled);

        this.filter = filter;
        this.meters = meters != null ? Immutables.wrap(meters) : Collections.<LogMeterConfiguration>emptyList();
        this.postFilter = postFilter;
        this.transformer = transformer;
        this.maxStackTraceDepth = maxStackTraceDepth;
        this.maxMessageSize = maxMessageSize;
        this.maxRate = maxRate;
        this.maxStackTraceRate = maxStackTraceRate;
        this.maxBundleSize = maxBundleSize;
    }

    public final LogFilterConfiguration getFilter() {
        return filter;
    }

    public final LogFilterConfiguration getPostFilter() {
        return postFilter;
    }

    public final List<LogMeterConfiguration> getMeters() {
        return meters;
    }

    public final LogProviderConfiguration getTransformer() {
        return transformer;
    }

    public int getMaxStackTraceDepth() {
        return maxStackTraceDepth;
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    public int getMaxRate() {
        return maxRate;
    }

    public int getMaxStackTraceRate() {
        return maxStackTraceRate;
    }

    public int getMaxBundleSize() {
        return maxBundleSize;
    }

    public List<MetricValueSchemaConfiguration> getMetricSchemas() {
        List<MetricValueSchemaConfiguration> schemas = new ArrayList<MetricValueSchemaConfiguration>(meters.size());
        for (LogMeterConfiguration meter : meters) {
            if (meter.getMeter() instanceof LogConfiguration)
                schemas.addAll(((LogConfiguration) meter.getMeter()).getMetricSchemas());
            else
                schemas.add(meter.getMeter().getSchema(meter.getMetricType()));
        }

        return schemas;
    }

    public void buildComponentSchemas(String metricType, Set<ComponentValueSchemaConfiguration> components) {
        for (LogMeterConfiguration meter : meters) {
            String prefix = getPrefix(metricType, meter.getMetricType());

            if (meter.getMeter() instanceof LogConfiguration)
                ((LogConfiguration) meter.getMeter()).buildComponentSchemas(prefix + meter.getMetricType(), components);
        }

        components.add(new ComponentValueSchemaConfiguration(metricType,
                Collections.singletonList(new ObjectValueSchemaConfiguration(metricType))));
    }

    @Override
    public MetricValueSchemaConfiguration getSchema(String metricType) {
        Assert.supports(false);
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LogConfiguration))
            return false;

        LogConfiguration configuration = (LogConfiguration) o;
        return super.equals(configuration) && Objects.equals(filter, configuration.filter) && meters.equals(configuration.meters) &&
                Objects.equals(postFilter, configuration.postFilter) && Objects.equals(transformer, configuration.transformer) &&
                maxStackTraceDepth == configuration.maxStackTraceDepth && maxMessageSize == configuration.maxMessageSize &&
                maxRate == configuration.maxRate && maxStackTraceRate == configuration.maxStackTraceRate &&
                maxBundleSize == configuration.maxBundleSize;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(filter, meters, postFilter, transformer, maxStackTraceDepth, maxMessageSize, maxRate,
                maxStackTraceRate, maxBundleSize);
    }

    @Override
    public String toString() {
        return "log";
    }

    public static String getPrefix(String metricType, String meterType) {
        String prefix1 = getPrefix(metricType);
        String prefix2 = getPrefix(meterType);

        if (!prefix1.equals(prefix2))
            return prefix1;
        else
            return "";
    }

    private static String getPrefix(String metricType) {
        String prefix = "";
        int pos = metricType.indexOf('.');
        if (pos != -1)
            prefix = metricType.substring(0, pos + 1);

        return prefix;
    }
}
