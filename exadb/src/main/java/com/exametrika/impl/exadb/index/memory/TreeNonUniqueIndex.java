/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.memory;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.ITreeIndex;
import com.exametrika.api.exadb.index.IValueConverter;
import com.exametrika.api.exadb.index.TreeIndexStatistics;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.impl.exadb.index.IndexManager;
import com.exametrika.impl.exadb.index.NonUniqueSortedIndex;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;


/**
 * The {@link TreeNonUniqueIndex} is a non-unique in-memory tree index based on specified unique in-memory tree index. Key:value pairs must still
 * have to be unique.
 *
 * @param <K> key type
 * @param <V> value type
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class TreeNonUniqueIndex<K, V> extends NonUniqueSortedIndex<K, V> implements ITreeIndex<K, V> {
    public TreeNonUniqueIndex(IndexManager indexManager, IndexSchemaConfiguration schema,
                              ITreeIndex<ByteArray, ByteArray> index, boolean fixedKey, int maxKeySize, int valueSize,
                              IKeyNormalizer<K> keyNormalizer, IValueConverter<V> valueConverter) {
        super(indexManager, schema, index, fixedKey, maxKeySize, valueSize, keyNormalizer, valueConverter);
    }

    @Override
    public TreeIndexStatistics getStatistics() {
        return ((ITreeIndex) getIndex()).getStatistics();
    }
}
