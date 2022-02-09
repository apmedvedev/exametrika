/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.bitmap;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;

/**
 * The {@link UnorderedTabularKeyNormalizer} is a key normalizer that uses tabular mapping of key on positive integer
 * value. Key normalizer does not preserve total order of keys on mapped values.
 *
 * @param <K> key type
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class UnorderedTabularKeyNormalizer<K> implements IKeyNormalizer<K> {
    private final IKeyNormalizer<K> sourceKeyNormalizer;
    private final IKeyNormalizer<Number> destinationKeyNormalizer;
    private final IUniqueIndex<ByteArray, Number> mappingIndex;
    private final ByteArray nullKey;
    private final boolean allowNulls;
    private long counter;

    public UnorderedTabularKeyNormalizer(IKeyNormalizer<K> sourceKeyNormalizer,
                                         IKeyNormalizer<Number> destinationKeyNormalizer, IUniqueIndex<ByteArray, Number> mappingIndex, ByteArray nullKey,
                                         boolean allowNulls, Number counter) {
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
    }

    @Override
    public ByteArray normalize(K key) {
        // TOD0: нужно где-то записывать значение изменившегося счетчика
        if (key == null) {
            Assert.isTrue(allowNulls);
            return nullKey;
        }

        ByteArray normalizedKey = sourceKeyNormalizer.normalize(key);
        Number value = mappingIndex.find(normalizedKey);
        if (value == null) {
            value = counter++;
            mappingIndex.add(normalizedKey, value);
        }

        return destinationKeyNormalizer.normalize(value);
    }
}
