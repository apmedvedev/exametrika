/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.meters;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.fields.instance.InstanceCollector;
import com.exametrika.impl.aggregator.common.values.NameValue;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.IFieldCollector;
import com.exametrika.spi.aggregator.common.meters.IGauge;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;


/**
 * The {@link Gauge} is a gauge.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Gauge implements IGauge {
    private static final ILogger logger = Loggers.get(Gauge.class);
    private final String metricType;
    private final IMeasurementProvider provider;
    private final IFieldCollector[] fieldCollectors;
    private final IInstanceContextProvider contextProvider;
    private boolean hasMeasurement;

    public Gauge(String metricType, IMeasurementProvider provider, IFieldCollector[] fieldCollectors,
                 IInstanceContextProvider contextProvider) {
        Assert.notNull(metricType);
        Assert.notNull(fieldCollectors);
        Assert.notNull(contextProvider);

        this.metricType = metricType;
        this.provider = provider;
        this.fieldCollectors = fieldCollectors;
        this.contextProvider = contextProvider;
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
    public NameValue extract(double approximationMultiplier, boolean force, boolean clear) {
        if (hasMeasurement) {
            List<IFieldValue> fields = new ArrayList<IFieldValue>(fieldCollectors.length);
            for (int i = 0; i < fieldCollectors.length; i++)
                fields.add(fieldCollectors[i].extract(0, approximationMultiplier, clear));

            if (clear)
                hasMeasurement = false;

            return new NameValue(fields);
        } else
            return null;
    }

    @Override
    public boolean hasInstanceFields() {
        for (IFieldCollector fieldCollector : fieldCollectors) {
            if (fieldCollector instanceof InstanceCollector)
                return true;
        }
        return false;
    }

    @Override
    public void setInstanceContext(JsonObject context) {
        contextProvider.setContext(context);
    }

    @Override
    public void measure(long value) {
        for (int i = 0; i < fieldCollectors.length; i++)
            fieldCollectors[i].update(value);

        hasMeasurement = true;
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
        measure(((Number) value).longValue());
    }
}
