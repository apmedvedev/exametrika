/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;


import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.values.StackValue;
import com.exametrika.spi.aggregator.common.meters.IFieldCollector;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.profiler.config.StackCounterConfiguration;


/**
 * The {@link StackCounter} is a stack counter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class StackCounter {
    private static final ILogger logger = Loggers.get(StackCounter.class);
    private final StackCounterConfiguration configuration;
    private final int index;
    private final IFieldCollector[] inherentFieldCollectors;
    private final IFieldCollector[] totalFieldCollectors;
    private final IMeasurementProvider provider;
    private long beginValue;
    private long childrenTotalDelta;
    private boolean enabled = true;

    public StackCounter(StackCounterConfiguration configuration, int index, IFieldCollector[] inherentFieldCollectors,
                        IFieldCollector[] totalFieldCollectors, IMeasurementProvider provider) {
        Assert.notNull(configuration);
        Assert.notNull(inherentFieldCollectors);
        Assert.notNull(totalFieldCollectors);
        Assert.notNull(provider);

        this.configuration = configuration;
        this.index = index;
        this.inherentFieldCollectors = inherentFieldCollectors;
        this.totalFieldCollectors = totalFieldCollectors;
        this.provider = provider;
    }

    public StackCounterConfiguration getConfiguration() {
        return configuration;
    }

    public int getIndex() {
        return index;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void beginMeasure() {
        beginValue = getValue();
        childrenTotalDelta = 0;
    }

    public void endMeasure(boolean disableInherent, StackCounter parent) {
        long value = getValue();
        long totalDelta = value - beginValue;
        long inherentDelta = totalDelta - childrenTotalDelta;

        if (!disableInherent) {
            for (int i = 0; i < inherentFieldCollectors.length; i++)
                inherentFieldCollectors[i].update(inherentDelta);
        }
        for (int i = 0; i < totalFieldCollectors.length; i++)
            totalFieldCollectors[i].update(totalDelta);

        if (parent != null)
            parent.childrenTotalDelta += totalDelta;

        beginValue = 0;
    }

    public StackValue extract(long count, double inherentApproximationMultiplier,
                              double totalApproximationMultiplier, boolean clear) {
        List<IFieldValue> inherentFields = new ArrayList<IFieldValue>(inherentFieldCollectors.length);
        for (int i = 0; i < inherentFieldCollectors.length; i++)
            inherentFields.add(inherentFieldCollectors[i].extract(count, inherentApproximationMultiplier, clear));

        List<IFieldValue> totalFields = new ArrayList<IFieldValue>(totalFieldCollectors.length);
        for (int i = 0; i < totalFieldCollectors.length; i++)
            totalFields.add(totalFieldCollectors[i].extract(0, totalApproximationMultiplier, clear));

        return new StackValue(inherentFields, totalFields);
    }

    private long getValue() {
        try {
            return ((Number) provider.getValue()).longValue();
        } catch (Exception e) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);

            return 0;
        }
    }
}
