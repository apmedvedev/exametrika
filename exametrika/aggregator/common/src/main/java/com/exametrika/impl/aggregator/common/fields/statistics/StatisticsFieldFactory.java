/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.fields.statistics;

import com.exametrika.impl.aggregator.common.values.StatisticsSerializer;
import com.exametrika.spi.aggregator.common.meters.IFieldCollector;
import com.exametrika.spi.aggregator.common.meters.IFieldFactory;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;


/**
 * The {@link StatisticsFieldFactory} is an implementation of {@link IFieldFactory} for statistics fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StatisticsFieldFactory implements IFieldFactory {
    @Override
    public IFieldCollector createCollector() {
        return new StatisticsCollector();
    }

    @Override
    public IFieldValueSerializer createValueSerializer() {
        return new StatisticsSerializer(false);
    }
}
