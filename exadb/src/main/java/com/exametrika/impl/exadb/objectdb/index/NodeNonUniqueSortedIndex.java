/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.index;

import com.exametrika.api.exadb.index.INonUniqueSortedIndex;
import com.exametrika.api.exadb.objectdb.INodeNonUniqueSortedIndex;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link NodeNonUniqueSortedIndex} implements {@link INodeNonUniqueSortedIndex}.
 *
 * @param <K> key type
 * @param <V> node type
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class NodeNonUniqueSortedIndex<K, V> extends NodeSortedIndex<K, V> implements INodeNonUniqueSortedIndex<K, V> {
    public NodeNonUniqueSortedIndex(IDatabaseContext context, INonUniqueSortedIndex<Object, Long> index) {
        super(context, index);
    }

    @Override
    public INonUniqueSortedIndex<Object, Long> getIndex() {
        return (INonUniqueSortedIndex<Object, Long>) super.getIndex();
    }

    @Override
    public Iterable<V> findValues(K key) {
        return new ValueIterable(getIndex().findValues(getKey(key)));
    }

    @Override
    public void remove(K key, Object value, boolean updateIndex, boolean updateCache) {
        if (key == null)
            return;

        getIndex().remove(getKey(key), getId(value));
    }
}
