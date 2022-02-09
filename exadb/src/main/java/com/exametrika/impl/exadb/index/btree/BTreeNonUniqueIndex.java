/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.btree;

import com.exametrika.api.exadb.index.BTreeIndexStatistics;
import com.exametrika.api.exadb.index.IBTreeIndex;
import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.IValueConverter;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.impl.exadb.index.IndexManager;
import com.exametrika.impl.exadb.index.NonUniqueSortedIndex;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;


/**
 * The {@link BTreeNonUniqueIndex} is a non-unique B+ tree index based on specified unique B+ tree index. Key:value pairs must still
 * have to be unique.
 *
 * @param <K> key type
 * @param <V> value type
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class BTreeNonUniqueIndex<K, V> extends NonUniqueSortedIndex<K, V> implements IBTreeIndex<K, V> {
    public BTreeNonUniqueIndex(IndexManager indexManager, IndexSchemaConfiguration schema,
                               IBTreeIndex<ByteArray, ByteArray> index, boolean fixedKey, int maxKeySize, int valueSize,
                               IKeyNormalizer<K> keyNormalizer, IValueConverter<V> valueConverter) {
        super(indexManager, schema, index, fixedKey, maxKeySize, valueSize, keyNormalizer, valueConverter);
    }

    @Override
    public BTreeIndexStatistics getStatistics() {
        return ((IBTreeIndex) getIndex()).getStatistics();
    }
}
