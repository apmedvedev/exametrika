/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.index;

import java.util.Iterator;

import com.exametrika.api.exadb.index.ISortedIndex;
import com.exametrika.api.exadb.objectdb.INodeSortedIndex;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link NodeSortedIndex} implements {@link INodeSortedIndex}.
 *
 * @param <K> key type
 * @param <V> node type
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class NodeSortedIndex<K, V> extends NodeIndex<K, V> implements INodeSortedIndex<K, V> {
    public NodeSortedIndex(IDatabaseContext context, ISortedIndex<Object, Long> index) {
        super(context, index);
    }

    @Override
    public ISortedIndex<Object, Long> getIndex() {
        return (ISortedIndex<Object, Long>) super.getIndex();
    }

    @Override
    public Pair<ByteArray, V> findFirst() {
        Pair<ByteArray, Long> pair = getIndex().findFirst();
        if (pair != null)
            return new Pair<ByteArray, V>(pair.getKey(), findById(pair.getValue()));
        else
            return null;
    }

    @Override
    public V findFirstValue() {
        Long id = getIndex().findFirstValue();
        if (id != null)
            return findById(id);
        else
            return null;
    }

    @Override
    public Pair<ByteArray, V> findLast() {
        Pair<ByteArray, Long> pair = getIndex().findLast();
        if (pair != null)
            return new Pair<ByteArray, V>(pair.getKey(), findById(pair.getValue()));
        else
            return null;
    }

    @Override
    public V findLastValue() {
        Long id = getIndex().findLastValue();
        if (id != null)
            return findById(id);
        else
            return null;
    }

    @Override
    public Pair<ByteArray, V> findFloor(K key, boolean inclusive) {
        Object indexKey;
        if (key != null)
            indexKey = getKey(key);
        else
            indexKey = null;

        Pair<ByteArray, Long> pair = getIndex().findFloor(indexKey, inclusive);
        if (pair != null)
            return new Pair<ByteArray, V>(pair.getKey(), findById(pair.getValue()));
        else
            return null;
    }

    @Override
    public V findFloorValue(K key, boolean inclusive) {
        Object indexKey;
        if (key != null)
            indexKey = getKey(key);
        else
            indexKey = null;

        Long id = getIndex().findFloorValue(indexKey, inclusive);
        if (id != null)
            return findById(id);
        else
            return null;
    }

    @Override
    public Pair<ByteArray, V> findCeiling(K key, boolean inclusive) {
        Object indexKey;
        if (key != null)
            indexKey = getKey(key);
        else
            indexKey = null;

        Pair<ByteArray, Long> pair = getIndex().findCeiling(indexKey, inclusive);
        if (pair != null)
            return new Pair<ByteArray, V>(pair.getKey(), findById(pair.getValue()));
        else
            return null;
    }

    @Override
    public V findCeilingValue(K key, boolean inclusive) {
        Object indexKey;
        if (key != null)
            indexKey = getKey(key);
        else
            indexKey = null;

        Long id = getIndex().findCeilingValue(indexKey, inclusive);
        if (id != null)
            return findById(id);
        else
            return null;
    }

    @Override
    public Iterable<Pair<ByteArray, V>> find(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        Object from;
        if (fromKey != null)
            from = getKey(fromKey);
        else
            from = null;

        Object to;
        if (toKey != null)
            to = getKey(toKey);
        else
            to = null;

        return new EntryIterable(getIndex().find(from, fromInclusive, to, toInclusive));
    }

    @Override
    public Iterable<V> findValues(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        Object from;
        if (fromKey != null)
            from = getKey(fromKey);
        else
            from = null;

        Object to;
        if (toKey != null)
            to = getKey(toKey);
        else
            to = null;

        return new ValueIterable(getIndex().findValues(from, fromInclusive, to, toInclusive));
    }

    @Override
    public long estimate(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        Object from;
        if (fromKey != null)
            from = getKey(fromKey);
        else
            from = null;

        Object to;
        if (toKey != null)
            to = getKey(toKey);
        else
            to = null;

        return getIndex().estimate(from, fromInclusive, to, toInclusive);
    }

    private class EntryIterable implements Iterable<Pair<ByteArray, V>> {
        private final Iterable<Pair<ByteArray, Long>> it;

        public EntryIterable(Iterable<Pair<ByteArray, Long>> it) {
            Assert.notNull(it);

            this.it = it;
        }

        @Override
        public Iterator<Pair<ByteArray, V>> iterator() {
            return new EntryIterator(it.iterator());
        }
    }

    private class EntryIterator implements Iterator<Pair<ByteArray, V>> {
        private final Iterator<Pair<ByteArray, Long>> it;

        public EntryIterator(Iterator<Pair<ByteArray, Long>> it) {
            Assert.notNull(it);

            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Pair<ByteArray, V> next() {
            Pair<ByteArray, Long> pair = it.next();
            return new Pair<ByteArray, V>(pair.getKey(), findById(pair.getValue()));
        }

        @Override
        public void remove() {
            it.remove();
        }
    }

    protected class ValueIterable implements Iterable<V> {
        private final Iterable<Long> it;

        public ValueIterable(Iterable<Long> it) {
            Assert.notNull(it);

            this.it = it;
        }

        @Override
        public Iterator<V> iterator() {
            return new ValueIterator(it.iterator());
        }
    }

    protected class ValueIterator implements Iterator<V> {
        private final Iterator<Long> it;

        public ValueIterator(Iterator<Long> it) {
            Assert.notNull(it);

            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public V next() {
            Long id = it.next();
            return findById(id);
        }

        @Override
        public void remove() {
            it.remove();
        }
    }
}
