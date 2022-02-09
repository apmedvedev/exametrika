/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.meters.config.MeterConfiguration;


/**
 * The {@link JmxAttributeConfiguration} is a configuration for JMX attribute.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JmxAttributeConfiguration extends Configuration {
    private final String metricType;
    private final MeterConfiguration meter;
    private final String attribute;
    private final String converterExpression;

    public JmxAttributeConfiguration(String metricType, MeterConfiguration meter, String attribute, String converterExpression) {
        Assert.notNull(metricType);
        Assert.notNull(meter);
        Assert.notNull(attribute);

        this.metricType = metricType;
        this.meter = meter;
        this.attribute = attribute;
        this.converterExpression = converterExpression;
    }

    public String getMetricType() {
        return metricType;
    }

    public MeterConfiguration getMeter() {
        return meter;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getConverterExpression() {
        return converterExpression;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JmxAttributeConfiguration))
            return false;

        JmxAttributeConfiguration configuration = (JmxAttributeConfiguration) o;
        return metricType.equals(configuration.metricType) &&
                meter.equals(configuration.meter) && attribute.equals(configuration.attribute) &&
                Objects.equals(converterExpression, configuration.converterExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(metricType, meter, attribute, converterExpression);
    }

    @Override
    public String toString() {
        return metricType;
    }
}
