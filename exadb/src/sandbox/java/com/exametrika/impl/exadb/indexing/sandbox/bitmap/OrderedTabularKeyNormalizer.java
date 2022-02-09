/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.bitmap;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.ISortedIndex;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;


/**
 * The {@link OrderedTabularKeyNormalizer} is a key normalizer that uses tabular mapping of key on positive integer value.
 * Key normalizer preserves total order of keys on mapped values (in some cases it is impossible to preserve total order of some key,
 * in that case normalizer marks particular key as out of order).
 *
 * @param <K> key type
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class OrderedTabularKeyNormalizer<K> implements IKeyNormalizer<K> {
    private final IKeyNormalizer<K> sourceKeyNormalizer;
    private final IKeyNormalizer<Number> destinationKeyNormalizer;
    private final ISortedIndex<ByteArray, Number> mappingIndex;
    private final ByteArray nullKey;
    private final boolean allowNulls;
    private long counter;
    private final int step;

    public OrderedTabularKeyNormalizer(IKeyNormalizer<K> sourceKeyNormalizer,
                                       IKeyNormalizer<Number> destinationKeyNormalizer, ISortedIndex<ByteArray, Number> mappingIndex, ByteArray nullKey,
                                       boolean allowNulls, Number counter, int step) {
        Assert.notNull(sourceKeyNormalizer);
        Assert.notNull(destinationKeyNormalizer);
        Assert.notNull(mappingIndex);
        Assert.notNull(nullKey);
        Assert.notNull(counter);

        this.sourceKeyNormalizer = sourceKeyNormalizer;
        this.destinationKeyNormalizer = destinationKeyNormalizer;
        this.mappingIndex = mappingIndex;
        this.nullKey = nullKey;
        this.allowNulls = allowNulls;
        this.counter = counter.longValue();
        this.step = step;
    }

    @Override
    public ByteArray normalize(K key) {
        if (key == null) {
            Assert.isTrue(allowNulls);
            return nullKey;
        }

        ByteArray normalizedKey = sourceKeyNormalizer.normalize(key);
        Number value = mappingIndex.find(normalizedKey);
        if (value != null)
            return destinationKeyNormalizer.normalize(value);
        else
            return appendKey(normalizedKey);
    }

    private ByteArray appendKey(ByteArray normalizedKey) {
        Number value = null;
        boolean outOfOrder = false;
        if (!mappingIndex.isEmpty()) {
            Number start = mappingIndex.findFloorValue(normalizedKey, false);
            Number end = mappingIndex.findCeilingValue(normalizedKey, false);

            if (start == null) {
                if (end.longValue() > 1)
                    value = end.longValue() / 2;
                else
                    outOfOrder = true;
            } else if (end == null) {
                value = start.longValue() + step;
                counter += step;
            } else {
                long interval = end.longValue() - start.longValue();
                if (interval > 1)
                    value = start.longValue() + interval / 2;
                else
                    outOfOrder = true;
            }
        } else {
            value = counter;
            counter += step;
        }

        if (outOfOrder) {
            value = counter;
            counter += step;
        }

        mappingIndex.add(normalizedKey, value);

        return destinationKeyNormalizer.normalize(value);
    }
}
