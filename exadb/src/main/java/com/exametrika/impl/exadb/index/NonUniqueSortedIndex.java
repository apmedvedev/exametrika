/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index;

import java.util.Arrays;
import java.util.Iterator;

import com.exametrika.api.exadb.core.config.CacheCategoryTypeConfiguration;
import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.INonUniqueSortedIndex;
import com.exametrika.api.exadb.index.ISortedIndex;
import com.exametrika.api.exadb.index.IValueConverter;
import com.exametrika.api.exadb.index.Indexes;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.rawdb.IRawBatchControl;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;


/**
 * The {@link NonUniqueSortedIndex} is a non-unique sorted index based on specified sorted index. Key:value pairs must still
 * have to be unique.
 *
 * @param <K> key type
 * @param <V> value type
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class NonUniqueSortedIndex<K, V> extends AbstractIndexSpace implements INonUniqueSortedIndex<K, V> {
    private static final ByteArray EMPTY_VALUE = new ByteArray(new byte[0]);
    private final ISortedIndex<ByteArray, ByteArray> index;
    private final int valueSize;
    private final IKeyNormalizer<K> keyNormalizer;
    private final IValueConverter<V> valueConverter;
    private final ByteArray minValue;
    private final ByteArray maxValue;
    private final boolean fixedKey;
    private final int maxKeySize;

    public NonUniqueSortedIndex(IndexManager indexManager, IndexSchemaConfiguration schema,
                                ISortedIndex<ByteArray, ByteArray> index, boolean fixedKey, int maxKeySize, int valueSize,
                                IKeyNormalizer<K> keyNormalizer, IValueConverter<V> valueConverter) {
        super(indexManager, schema, index.getId());

        Assert.notNull(index);
        Assert.isTrue(index instanceof AbstractIndexSpace);
        Assert.notNull(keyNormalizer);
        Assert.notNull(valueConverter);

        this.index = index;
        this.fixedKey = fixedKey;
        this.maxKeySize = maxKeySize;
        this.valueSize = valueSize;

        byte[] buffer = new byte[valueSize];
        Arrays.fill(buffer, (byte) 0x0);
        this.minValue = new ByteArray(buffer);

        buffer = new byte[valueSize];
        Arrays.fill(buffer, (byte) 0xFF);
        this.maxValue = new ByteArray(buffer);
        this.keyNormalizer = keyNormalizer;
        this.valueConverter = valueConverter;
    }

    public ISortedIndex<ByteArray, ByteArray> getIndex() {
        return index;
    }

    @Override
    public boolean isEmpty() {
        return index.isEmpty();
    }

    @Override
    public long getCount() {
        return index.getCount();
    }

    @Override
    public V find(K key) {
        Iterator<V> it = findValues(key).iterator();
        if (it.hasNext())
            return it.next();
        else
            return null;
    }

    @Override
    public void add(K key, V value) {
        Assert.notNull(key);
        Assert.notNull(value);

        ByteArray byteKey = keyNormalizer.normalize(key);
        ByteArray byteValue = valueConverter.toByteArray(value);
        index.add(createCombinedKey(byteKey, byteValue), EMPTY_VALUE);
    }

    @Override
    public void bulkAdd(Iterable<Pair<K, V>> elements) {
        Assert.notNull(elements);

        index.bulkAdd(new BulkIterable(elements));
    }

    @Override
    public void remove(K key) {
        Assert.notNull(key);

        ByteArray byteKey = keyNormalizer.normalize(key);
        ByteArray fromKey = createCombinedKey(byteKey, minValue);
        ByteArray toKey = createCombinedKey(byteKey, maxValue);
        while (true) {
            Iterator<Pair<ByteArray, ByteArray>> it = index.find(fromKey, true, toKey, true).iterator();
            if (!it.hasNext())
                break;

            Pair<ByteArray, ByteArray> pair = it.next();
            index.remove(pair.getKey());
        }
    }

    @Override
    public void clear() {
        index.clear();
    }

    @Override
    public ByteArray normalize(K key) {
        Assert.notNull(key);

        if (fixedKey)
            return keyNormalizer.normalize(key);
        else {
            ByteOutputStream stream = new ByteOutputStream();

            Indexes.normalizeComposite(stream, keyNormalizer.normalize(key));
            stream.write(0x0);
            stream.write(0x0);

            return new ByteArray(stream.getBuffer(), 0, stream.getLength());
        }
    }

    @Override
    public Pair<ByteArray, V> findFloor(K key, boolean inclusive) {
        ByteArray byteKey;
        if (key != null) {
            ByteArray toByteKey = keyNormalizer.normalize(key);
            if (inclusive)
                byteKey = createCombinedKey(toByteKey, maxValue);
            else
                byteKey = createCombinedKey(toByteKey, minValue);
        } else
            byteKey = null;

        Pair<ByteArray, ByteArray> element = index.findFloor(byteKey, inclusive);
        if (element != null)
            return createElement(element.getKey());
        else
            return null;
    }

    @Override
    public V findFloorValue(K key, boolean inclusive) {
        ByteArray byteKey;
        if (key != null) {
            ByteArray toByteKey = keyNormalizer.normalize(key);
            if (inclusive)
                byteKey = createCombinedKey(toByteKey, maxValue);
            else
                byteKey = createCombinedKey(toByteKey, minValue);
        } else
            byteKey = null;

        Pair<ByteArray, ByteArray> element = index.findFloor(byteKey, inclusive);
        if (element != null)
            return createValue(element.getKey());
        else
            return null;
    }

    @Override
    public Pair<ByteArray, V> findCeiling(K key, boolean inclusive) {
        ByteArray byteKey;
        if (key != null) {
            ByteArray toByteKey = keyNormalizer.normalize(key);
            if (inclusive)
                byteKey = createCombinedKey(toByteKey, minValue);
            else
                byteKey = createCombinedKey(toByteKey, maxValue);
        } else
            byteKey = null;

        Pair<ByteArray, ByteArray> element = index.findCeiling(byteKey, inclusive);
        if (element != null)
            return createElement(element.getKey());
        else
            return null;
    }

    @Override
    public V findCeilingValue(K key, boolean inclusive) {
        ByteArray byteKey;
        if (key != null) {
            ByteArray toByteKey = keyNormalizer.normalize(key);
            if (inclusive)
                byteKey = createCombinedKey(toByteKey, minValue);
            else
                byteKey = createCombinedKey(toByteKey, maxValue);
        } else
            byteKey = null;

        Pair<ByteArray, ByteArray> element = index.findCeiling(byteKey, inclusive);
        if (element != null)
            return createValue(element.getKey());
        else
            return null;
    }

    @Override
    public Pair<ByteArray, V> findFirst() {
        Pair<ByteArray, ByteArray> pair = index.findFirst();
        if (pair == null)
            return null;

        return createElement(pair.getKey());
    }

    @Override
    public V findFirstValue() {
        Pair<ByteArray, ByteArray> pair = index.findFirst();
        if (pair == null)
            return null;

        return createValue(pair.getKey());
    }

    @Override
    public Pair<ByteArray, V> findLast() {
        Pair<ByteArray, ByteArray> pair = index.findLast();
        if (pair == null)
            return null;

        return createElement(pair.getKey());
    }

    @Override
    public V findLastValue() {
        Pair<ByteArray, ByteArray> pair = index.findLast();
        if (pair == null)
            return null;

        return createValue(pair.getKey());
    }

    @Override
    public Iterable<Pair<ByteArray, V>> find(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        ByteArray from;
        if (fromKey != null) {
            ByteArray fromByteKey = keyNormalizer.normalize(fromKey);
            if (fromInclusive)
                from = createCombinedKey(fromByteKey, minValue);
            else
                from = createCombinedKey(fromByteKey, maxValue);
        } else
            from = null;

        ByteArray to;
        if (toKey != null) {
            ByteArray toByteKey = keyNormalizer.normalize(toKey);
            if (toInclusive)
                to = createCombinedKey(toByteKey, maxValue);
            else
                to = createCombinedKey(toByteKey, minValue);
        } else
            to = null;

        return new NonUniqueIterable(index.find(from, fromInclusive, to, toInclusive));
    }

    @Override
    public Iterable<V> findValues(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        ByteArray from;
        if (fromKey != null) {
            ByteArray fromByteKey = keyNormalizer.normalize(fromKey);
            if (fromInclusive)
                from = createCombinedKey(fromByteKey, minValue);
            else
                from = createCombinedKey(fromByteKey, maxValue);
        } else
            from = null;

        ByteArray to;
        if (toKey != null) {
            ByteArray toByteKey = keyNormalizer.normalize(toKey);
            if (toInclusive)
                to = createCombinedKey(toByteKey, maxValue);
            else
                to = createCombinedKey(toByteKey, minValue);
        } else
            to = null;

        return new NonUniqueValueIterable(index.find(from, fromInclusive, to, toInclusive));
    }

    @Override
    public long estimate(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        ByteArray from;
        if (fromKey != null) {
            ByteArray fromByteKey = keyNormalizer.normalize(fromKey);
            if (fromInclusive)
                from = createCombinedKey(fromByteKey, minValue);
            else
                from = createCombinedKey(fromByteKey, maxValue);
        } else
            from = null;

        ByteArray to;
        if (toKey != null) {
            ByteArray toByteKey = keyNormalizer.normalize(toKey);
            if (toInclusive)
                to = createCombinedKey(toByteKey, maxValue);
            else
                to = createCombinedKey(toByteKey, minValue);
        } else
            to = null;

        return index.estimate(from, fromInclusive, to, toInclusive);
    }

    @Override
    public Pair<ByteArray, Long> rebuildStatistics(IRawBatchControl batchControl, Pair<ByteArray, Long> startBin,
                                                   double keyRatio, long rebuildThreshold, boolean force) {
        return index.rebuildStatistics(batchControl, startBin, keyRatio, rebuildThreshold, force);
    }

    @Override
    public Iterable<V> findValues(K key) {
        return findValues(key, true, key, true);
    }

    @Override
    public void remove(K key, V value) {
        Assert.notNull(key);
        Assert.notNull(value);

        ByteArray byteKey = keyNormalizer.normalize(key);
        ByteArray byteValue = valueConverter.toByteArray(value);
        index.remove(createCombinedKey(byteKey, byteValue));
    }

    @Override
    public void onTransactionCommitted() {
        ((AbstractIndexSpace) index).onTransactionCommitted();
    }

    @Override
    public void onTransactionRolledBack() {
        ((AbstractIndexSpace) index).onTransactionRolledBack();
    }

    @Override
    public void delete() {
        ((AbstractIndexSpace) index).delete();
    }

    @Override
    public void flush(boolean full) {
        ((AbstractIndexSpace) index).flush(full);
    }

    @Override
    public void unload(boolean full) {
        ((AbstractIndexSpace) index).unload(full);
    }

    @Override
    public void assertValid() {
        ((AbstractIndexSpace) index).assertValid();
    }

    @Override
    public String printStatistics() {
        return ((AbstractIndexSpace) index).printStatistics();
    }

    @Override
    public CacheCategoryTypeConfiguration getCategoryTypeConfiguration() {
        return ((AbstractIndexSpace) index).getCategoryTypeConfiguration();
    }

    @Override
    public void setCategoryTypeConfiguration(CacheCategoryTypeConfiguration defaultCacheCategoryType) {
        ((AbstractIndexSpace) index).setCategoryTypeConfiguration(defaultCacheCategoryType);
    }

    @Override
    public String toString() {
        return index.toString();
    }

    private ByteArray createCombinedKey(ByteArray key, ByteArray value) {
        Assert.notNull(key);
        Assert.isTrue((fixedKey && key.getLength() == maxKeySize) || (!fixedKey && key.getLength() <= maxKeySize));
        Assert.isTrue(value.getLength() == valueSize);

        ByteOutputStream stream = new ByteOutputStream();
        if (fixedKey)
            Indexes.normalizeFixedComposite(stream, key);
        else {
            Indexes.normalizeComposite(stream, key);
            stream.write(0x0);
            stream.write(0x0);
        }

        stream.write(value.getBuffer(), value.getOffset(), value.getLength());

        return new ByteArray(stream.getBuffer(), 0, stream.getLength());
    }

    private Pair<ByteArray, V> createElement(ByteArray combinedKey) {
        ByteArray key = new ByteArray(combinedKey.getBuffer(), combinedKey.getOffset(),
                combinedKey.getLength() - valueSize);
        V value = valueConverter.toValue(new ByteArray(combinedKey.getBuffer(), combinedKey.getOffset() +
                combinedKey.getLength() - valueSize, valueSize));
        return new Pair<ByteArray, V>(key, value);
    }

    private V createValue(ByteArray combinedKey) {
        return valueConverter.toValue(new ByteArray(combinedKey.getBuffer(), combinedKey.getOffset() +
                combinedKey.getLength() - valueSize, valueSize));
    }

    private class BulkIterable implements Iterable<Pair<ByteArray, ByteArray>> {
        private final Iterable<Pair<K, V>> it;

        public BulkIterable(Iterable<Pair<K, V>> it) {
            Assert.notNull(it);

            this.it = it;
        }

        @Override
        public Iterator<Pair<ByteArray, ByteArray>> iterator() {
            return new BulkIterator(it.iterator());
        }
    }

    private class BulkIterator implements Iterator<Pair<ByteArray, ByteArray>> {
        private final Iterator<Pair<K, V>> it;

        public BulkIterator(Iterator<Pair<K, V>> it) {
            Assert.notNull(it);

            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Pair<ByteArray, ByteArray> next() {
            Pair<K, V> pair = it.next();
            ByteArray byteKey = keyNormalizer.normalize(pair.getKey());
            ByteArray byteValue = valueConverter.toByteArray(pair.getValue());
            return new Pair<ByteArray, ByteArray>(createCombinedKey(byteKey, byteValue), EMPTY_VALUE);
        }

        @Override
        public void remove() {
            it.remove();
        }
    }

    private class NonUniqueIterable implements Iterable<Pair<ByteArray, V>> {
        private final Iterable<Pair<ByteArray, ByteArray>> it;

        public NonUniqueIterable(Iterable<Pair<ByteArray, ByteArray>> it) {
            Assert.notNull(it);

            this.it = it;
        }

        @Override
        public Iterator<Pair<ByteArray, V>> iterator() {
            return new NonUniqueIterator(it.iterator());
        }
    }

    private class NonUniqueIterator implements Iterator<Pair<ByteArray, V>> {
        private final Iterator<Pair<ByteArray, ByteArray>> it;

        public NonUniqueIterator(Iterator<Pair<ByteArray, ByteArray>> it) {
            Assert.notNull(it);

            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Pair<ByteArray, V> next() {
            return createElement(it.next().getKey());
        }

        @Override
        public void remove() {
            it.remove();
        }
    }

    private class NonUniqueValueIterable implements Iterable<V> {
        private final Iterable<Pair<ByteArray, ByteArray>> it;

        public NonUniqueValueIterable(Iterable<Pair<ByteArray, ByteArray>> it) {
            Assert.notNull(it);

            this.it = it;
        }

        @Override
        public Iterator<V> iterator() {
            return new NonUniqueValueIterator(it.iterator());
        }
    }

    private class NonUniqueValueIterator implements Iterator<V> {
        private final Iterator<Pair<ByteArray, ByteArray>> it;

        public NonUniqueValueIterator(Iterator<Pair<ByteArray, ByteArray>> it) {
            Assert.notNull(it);

            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public V next() {
            return createValue(it.next().getKey());
        }

        @Override
        public void remove() {
            it.remove();
        }
    }
}
