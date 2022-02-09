/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.util.Arrays;
import java.util.Iterator;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.ISortedIndex;
import com.exametrika.api.exadb.index.config.schema.ByteArrayKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.FixedCompositeKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.INodeSortedIndex;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobIndexSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link StructuredBlobSortedIndex} implements {@link INodeIndex}.
 *
 * @param <K> key type
 * @param <V> node type
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StructuredBlobSortedIndex<K, V> extends StructuredBlobIndex<K, V> implements INodeSortedIndex<K, V> {
    private final ByteArray prefix;
    private final ByteArray minKey;
    private final ByteArray maxKey;

    public StructuredBlobSortedIndex(StructuredBlobIndexSchemaConfiguration configuration, IDatabaseContext context,
                                     ISortedIndex<ByteArray, Long> index, StructuredBlobField field) {
        super(configuration, context, index, field);

        IKeyNormalizer boundsKeyNormalizer = new FixedCompositeKeyNormalizerSchemaConfiguration(Arrays.asList(
                new NumericKeyNormalizerSchemaConfiguration(DataType.LONG), new NumericKeyNormalizerSchemaConfiguration(DataType.SHORT),
                new ByteArrayKeyNormalizerSchemaConfiguration())).createKeyNormalizer();

        byte[] buffer;
        if (configuration.isFixedKey())
            buffer = new byte[configuration.getMaxKeySize()];
        else
            buffer = new byte[1];

        prefix = boundsKeyNormalizer.normalize(Arrays.asList(field.getNode().getId(), field.getSchema().getIndex(), new ByteArray(0)));
        minKey = boundsKeyNormalizer.normalize(Arrays.asList(field.getNode().getId(), field.getSchema().getIndex(), new ByteArray(buffer)));

        buffer = new byte[configuration.getMaxKeySize()];
        Arrays.fill(buffer, (byte) 0xFF);

        maxKey = boundsKeyNormalizer.normalize(Arrays.asList(field.getNode().getId(), field.getSchema().getIndex(), new ByteArray(buffer)));
    }

    @Override
    public ISortedIndex<ByteArray, Long> getIndex() {
        return (ISortedIndex<ByteArray, Long>) super.getIndex();
    }

    @Override
    public final Pair<ByteArray, V> findFirst() {
        Pair<ByteArray, Long> pair = getIndex().findCeiling(minKey, true);
        if (pair != null && pair.getKey().startsWith(prefix))
            return new Pair<ByteArray, V>(pair.getKey(), findById(pair.getValue()));
        else
            return null;
    }

    @Override
    public final V findFirstValue() {
        Pair<ByteArray, V> pair = findFirst();
        if (pair != null)
            return pair.getValue();
        else
            return null;
    }

    @Override
    public final Pair<ByteArray, V> findLast() {
        Pair<ByteArray, Long> pair = getIndex().findFloor(maxKey, true);
        if (pair != null && pair.getKey().startsWith(prefix))
            return new Pair<ByteArray, V>(pair.getKey(), findById(pair.getValue()));
        else
            return null;
    }

    @Override
    public final V findLastValue() {
        Pair<ByteArray, V> pair = findLast();
        if (pair != null)
            return pair.getValue();
        else
            return null;
    }

    @Override
    public final Pair<ByteArray, V> findFloor(K key, boolean inclusive) {
        ByteArray indexKey;
        if (key != null)
            indexKey = getKey(key);
        else {
            Pair<ByteArray, V> pair = findLast();
            if (pair != null)
                indexKey = pair.getKey();
            else
                return null;
        }

        Pair<ByteArray, Long> pair = getIndex().findFloor(indexKey, inclusive);
        if (pair != null && pair.getKey().startsWith(prefix))
            return new Pair<ByteArray, V>(pair.getKey(), findById(pair.getValue()));
        else
            return null;
    }

    @Override
    public final V findFloorValue(K key, boolean inclusive) {
        Pair<ByteArray, V> pair = findFloor(key, inclusive);
        if (pair != null)
            return pair.getValue();
        else
            return null;
    }

    @Override
    public final Pair<ByteArray, V> findCeiling(K key, boolean inclusive) {
        ByteArray indexKey;
        if (key != null)
            indexKey = getKey(key);
        else {
            Pair<ByteArray, V> pair = findFirst();
            if (pair != null)
                indexKey = pair.getKey();
            else
                return null;
        }

        Pair<ByteArray, Long> pair = getIndex().findCeiling(indexKey, inclusive);
        if (pair != null && pair.getKey().startsWith(prefix))
            return new Pair<ByteArray, V>(pair.getKey(), findById(pair.getValue()));
        else
            return null;
    }

    @Override
    public final V findCeilingValue(K key, boolean inclusive) {
        Pair<ByteArray, V> pair = findCeiling(key, inclusive);
        if (pair != null)
            return pair.getValue();
        else
            return null;
    }

    @Override
    public final Iterable<Pair<ByteArray, V>> find(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        ByteArray from;
        if (fromKey != null)
            from = getKey(fromKey);
        else {
            Pair<ByteArray, V> pair = findFirst();
            if (pair != null)
                from = pair.getKey();
            else
                return null;
        }

        ByteArray to;
        if (toKey != null)
            to = getKey(toKey);
        else {
            Pair<ByteArray, V> pair = findLast();
            if (pair != null)
                to = pair.getKey();
            else
                return null;
        }

        return new EntryIterable(getIndex().find(from, fromInclusive, to, toInclusive));
    }

    @Override
    public final Iterable<V> findValues(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        ByteArray from;
        if (fromKey != null)
            from = getKey(fromKey);
        else {
            Pair<ByteArray, V> pair = findFirst();
            if (pair != null)
                from = pair.getKey();
            else
                return null;
        }

        ByteArray to;
        if (toKey != null)
            to = getKey(toKey);
        else {
            Pair<ByteArray, V> pair = findLast();
            if (pair != null)
                to = pair.getKey();
            else
                return null;
        }

        return new ValueIterable(getIndex().findValues(from, fromInclusive, to, toInclusive));
    }

    @Override
    public final long estimate(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        ByteArray from;
        if (fromKey != null)
            from = getKey(fromKey);
        else {
            Pair<ByteArray, V> pair = findFirst();
            if (pair != null)
                from = pair.getKey();
            else
                return 0;
        }

        ByteArray to;
        if (toKey != null)
            to = getKey(toKey);
        else {
            Pair<ByteArray, V> pair = findLast();
            if (pair != null)
                to = pair.getKey();
            else
                return 0;
        }

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
            Assert.supports(false);
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
            Assert.supports(false);
        }
    }
}
