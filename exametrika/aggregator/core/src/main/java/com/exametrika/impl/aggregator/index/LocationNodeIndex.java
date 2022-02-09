/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.index;

import java.util.Arrays;

import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.impl.aggregator.Period;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link LocationNodeIndex} implements {@link INodeIndex}.
 *
 * @param <K> key type
 * @param <V> node type
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class LocationNodeIndex<K, V> extends PeriodNodeIndex<K, V> {
    private final int nodeSchemaIndex;

    public LocationNodeIndex(IDatabaseContext context, IUniqueIndex<Object, Long> index, Period period,
                             int i, int nodeSchemaIndex) {
        super(context, index, true, period, i);

        this.nodeSchemaIndex = nodeSchemaIndex;
    }

    @Override
    protected Object getKey(K key) {
        return Arrays.asList(period.getPeriodIndex(), nodeSchemaIndex, key);
    }
}
