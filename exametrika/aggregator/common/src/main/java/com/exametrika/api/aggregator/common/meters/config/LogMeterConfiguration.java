/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.meters.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.meters.config.LogFilterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogProviderConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.MeterConfiguration;


/**
 * The {@link LogMeterConfiguration} is a log meter configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class LogMeterConfiguration extends Configuration {
    private final String metricType;
    private final MeterConfiguration meter;
    private final LogFilterConfiguration filter;
    private final LogProviderConfiguration provider;

    public LogMeterConfiguration(String metricType, MeterConfiguration meter, LogFilterConfiguration filter, LogProviderConfiguration provider) {
        Assert.notNull(metricType);
        Assert.notNull(meter);

        this.metricType = metricType;
        this.meter = meter;
        this.filter = filter;
        this.provider = provider;
    }

    public String getMetricType() {
        return metricType;
    }

    public MeterConfiguration getMeter() {
        return meter;
    }

    public LogFilterConfiguration getFilter() {
        return filter;
    }

    public LogProviderConfiguration getProvider() {
        return provider;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof LogMeterConfiguration))
            return false;

        LogMeterConfiguration configuration = (LogMeterConfiguration) o;
        return metricType.equals(configuration.metricType) && meter.equals(configuration.meter) &&
                Objects.equals(filter, configuration.filter) && Objects.equals(provider, configuration.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(metricType, meter, filter, provider);
    }

    @Override
    public String toString() {
        return metricType;
    }
}
