/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IStatisticsValue;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;


/**
 * The {@link StatisticsSerializer} is an implementation of {@link IFieldValueSerializer} for {@link StatisticsValue}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StatisticsSerializer implements IFieldValueSerializer {
    private final boolean builder;

    public StatisticsSerializer(boolean builder) {
        this.builder = builder;
    }

    @Override
    public void serialize(IDataSerialization serialization, IFieldValue value) {
        IStatisticsValue statisticsValue = (IStatisticsValue) value;
        serialization.writeDouble(statisticsValue.getSumSquares());
    }

    @Override
    public IFieldValue deserialize(IDataDeserialization deserialization) {
        double sumSquares = deserialization.readDouble();

        if (builder)
            return new StatisticsBuilder(sumSquares);
        else
            return new StatisticsValue(sumSquares);
    }
}
