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
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.aggregator.common.fields.instance.InstanceCollector;
import com.exametrika.impl.aggregator.common.values.NameValue;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.ICounter;
import com.exametrika.spi.aggregator.common.meters.IFieldCollector;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.IStandardFieldCollector;


/**
 * The {@link Counter} is a counter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Counter implements ICounter {
    private static final ILogger logger = Loggers.get(Counter.class);
    private final String metricType;
    private final IMeasurementProvider provider;
    private final boolean useDeltas;
    private final IFieldCollector[] fieldCollectors;
    private final IInstanceContextProvider contextProvider;
    private long count;
    private long value = -1;
    private boolean hasMeasurement;
    private final MovingAverage movingAverage;

    public Counter(String metricType, IMeasurementProvider provider, boolean useDeltas, int smoothingSize,
                   IFieldCollector[] fieldCollectors, IInstanceContextProvider contextProvider) {
        Assert.notNull(metricType);
        Assert.notNull(fieldCollectors);
        Assert.notNull(contextProvider);

        this.metricType = metricType;
        this.provider = provider;
        this.useDeltas = useDeltas;
        this.fieldCollectors = fieldCollectors;
        this.contextProvider = contextProvider;
        if (smoothingSize != 0)
            movingAverage = new MovingAverage(smoothingSize);
        else
            movingAverage = null;
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
    public void beginMeasure(long value) {
        this.value = value;
    }

    @Override
    public void endMeasure(long value) {
        long delta = smooth(value - this.value);

        for (int i = 0; i < fieldCollectors.length; i++)
            fieldCollectors[i].update(delta);

        hasMeasurement = true;
    }

    @Override
    public void measure(long value) {
        if (this.value >= 0) {
            long delta = smooth(value - this.value);

            for (int i = 0; i < fieldCollectors.length; i++)
                fieldCollectors[i].update(delta);

            hasMeasurement = true;
        }

        this.value = value;
    }

    @Override
    public void measureDelta(long value) {
        value = smooth(value);
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

        if (value instanceof Pair) {
            Pair<Number, Number> pair = (Pair<Number, Number>) value;
            measure(pair.getKey().longValue(), pair.getValue().longValue());
        } else if (value != null) {
            long longValue = ((Number) value).longValue();

            if (!useDeltas)
                measure(longValue);
            else
                measureDelta(longValue);
        } else
            Assert.error();
    }

    private void measure(long count, long value) {
        if (count > this.count && this.value >= 0) {
            long deltaCount = count - this.count;
            long delta = smooth(value - this.value);

            for (int i = 0; i < fieldCollectors.length; i++) {
                IFieldCollector fieldCollector = fieldCollectors[i];
                if (fieldCollector instanceof IStandardFieldCollector)
                    ((IStandardFieldCollector) fieldCollector).update(deltaCount, delta);
                else
                    fieldCollector.update(delta);
            }

            hasMeasurement = true;
        }

        this.value = value;
        this.count = count;
    }

    private long smooth(long value) {
        if (movingAverage == null)
            return value;
        else
            return (long) movingAverage.next(value);
    }
}
