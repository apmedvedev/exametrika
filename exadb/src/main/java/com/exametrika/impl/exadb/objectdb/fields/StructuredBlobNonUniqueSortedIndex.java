/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.index.INonUniqueSortedIndex;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.INodeNonUniqueSortedIndex;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobIndexSchemaConfiguration;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link StructuredBlobNonUniqueSortedIndex} implements {@link INodeIndex}.
 *
 * @param <K> key type
 * @param <V> node type
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StructuredBlobNonUniqueSortedIndex<K, V> extends StructuredBlobSortedIndex<K, V> implements INodeNonUniqueSortedIndex<K, V> {
    public StructuredBlobNonUniqueSortedIndex(StructuredBlobIndexSchemaConfiguration configuration, IDatabaseContext context,
                                              INonUniqueSortedIndex<ByteArray, Long> index, StructuredBlobField field) {
        super(configuration, context, index, field);
    }

    @Override
    public INonUniqueSortedIndex<ByteArray, Long> getIndex() {
        return (INonUniqueSortedIndex<ByteArray, Long>) super.getIndex();
    }

    @Override
    public Iterable<V> findValues(K key) {
        return new ValueIterable(getIndex().findValues(getKey(key)));
    }
}
