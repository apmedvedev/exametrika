/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.meters;

import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.values.ObjectValue;
import com.exametrika.spi.aggregator.common.meters.IInfo;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;


/**
 * The {@link Info} is a informational meter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Info implements IInfo {
    private static final ILogger logger = Loggers.get(Info.class);
    private final String metricType;
    private final IMeasurementProvider provider;
    private Object value;
    private boolean hasMeasurement;

    public Info(String metricType, IMeasurementProvider provider) {
        Assert.notNull(metricType);

        this.metricType = metricType;
        this.provider = provider;
    }

    @Override
    public String getMetricType() {
        return metricType;
    }

    @Override
    public boolean hasProvider() {
        return provider != null;
    }

    @Override
    public void measure() {
        Assert.notNull(provider);

        Object value = null;
        try {
            value = provider.getValue();
        } catch (Exception e) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        }

        if (value != null)
            measure(value);
    }

    @Override
    public void measure(Object value) {
        Assert.notNull(value);
        value = JsonUtils.checkValue(value);

        hasMeasurement = true;
        this.value = value;
    }

    @Override
    public ObjectValue extract(double approximationMultiplier, boolean force, boolean clear) {
        if (value != null && (hasMeasurement || force)) {
            if (clear)
                hasMeasurement = false;

            return new ObjectValue(value);
        } else
            return null;
    }
}
