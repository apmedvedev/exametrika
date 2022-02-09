/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.index;

import java.util.Arrays;

import com.exametrika.api.exadb.index.INonUniqueSortedIndex;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.Period;
import com.exametrika.impl.exadb.objectdb.index.NodeNonUniqueSortedIndex;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link PeriodNodeNonUniqueSortedIndex} implements {@link INodeIndex}.
 *
 * @param <K> key type
 * @param <V> node type
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodNodeNonUniqueSortedIndex<K, V> extends NodeNonUniqueSortedIndex<K, V> {
    private final Period period;
    private final int i;

    public PeriodNodeNonUniqueSortedIndex(IDatabaseContext context, INonUniqueSortedIndex<Object, Long> index, Period period, int i) {
        super(context, index);

        Assert.notNull(period);

        this.period = period;
        this.i = i;
    }

    @Override
    protected V findById(long id) {
        return period.findNodeById(id);
    }

    @Override
    protected long getId(Object value) {
        return ((INode) value).getId();
    }

    @Override
    protected Object getKey(K key) {
        return Arrays.asList(period.getPeriodIndex(), key);
    }

    @Override
    protected IUniqueIndex<Object, Long> refreshIndex(int id) {
        return period.getSpace().getIndex(i).index;
    }
}
