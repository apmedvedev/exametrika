/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.fields.histogram;

import com.exametrika.api.aggregator.common.meters.config.CustomHistogramFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.HistogramFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.LogarithmicHistogramFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.UniformHistogramFieldConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.values.HistogramSerializer;
import com.exametrika.spi.aggregator.common.meters.IFieldCollector;
import com.exametrika.spi.aggregator.common.meters.IFieldFactory;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;


/**
 * The {@link HistogramFieldFactory} is an implementation of {@link IFieldFactory} for histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HistogramFieldFactory implements IFieldFactory {
    private final HistogramFieldConfiguration configuration;

    public HistogramFieldFactory(HistogramFieldConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    public IFieldCollector createCollector() {
        if (configuration instanceof UniformHistogramFieldConfiguration) {
            UniformHistogramFieldConfiguration uniformConfiguration = (UniformHistogramFieldConfiguration) configuration;
            return new UniformHistogramCollector(uniformConfiguration.getMinBound(), uniformConfiguration.getMaxBound(),
                    uniformConfiguration.getBinCount());
        } else if (configuration instanceof LogarithmicHistogramFieldConfiguration) {
            LogarithmicHistogramFieldConfiguration logarithmicConfiguration = (LogarithmicHistogramFieldConfiguration) configuration;
            return new LogarithmicHistogramCollector(logarithmicConfiguration.getMinBound(), logarithmicConfiguration.getBinCount());
        } else if (configuration instanceof CustomHistogramFieldConfiguration) {
            CustomHistogramFieldConfiguration customConfiguration = (CustomHistogramFieldConfiguration) configuration;
            long[] bounds = new long[customConfiguration.getBounds().size()];
            for (int i = 0; i < bounds.length; i++)
                bounds[i] = customConfiguration.getBounds().get(i);
            return new CustomHistogramCollector(bounds);
        } else
            return Assert.error();
    }

    @Override
    public IFieldValueSerializer createValueSerializer() {
        return new HistogramSerializer(false, configuration.getBinCount());
    }
}
