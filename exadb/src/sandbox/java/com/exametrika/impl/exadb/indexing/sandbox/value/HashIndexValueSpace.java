/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.value;

import java.util.Map;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.index.IndexManager;
import com.exametrika.impl.exadb.index.memory.HashIndexSpace;
import com.exametrika.impl.exadb.indexing.sandbox.IIndexValue;
import com.exametrika.spi.exadb.core.IDataFileAllocator;
import com.exametrika.spi.exadb.core.ITransactionProvider;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;


/**
 * The {@link HashIndexValueSpace} is a key-value space based on HashIndex.
 *
 * @param <K> key type
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class HashIndexValueSpace<K> extends IndexValueSpace<K> {
    private HashIndexSpace index;

    public static <K> HashIndexValueSpace create(IndexManager indexManager, IndexSchemaConfiguration configuration,
                                                 ITransactionProvider transactionProvider, IDataFileAllocator fileAllocator,
                                                 int fileIndex, String filePrefix, boolean fixedKey, int maxKeySize, int maxValueSize,
                                                 IKeyNormalizer<K> keyNormalizer, Map<String, String> properties) {
        Assert.notNull(transactionProvider);

        IRawTransaction transaction = transactionProvider.getRawTransaction();

        bindFile(transaction, fileIndex, filePrefix + "-values", configuration.getPathIndex());

        HashIndexValueSpace space = new HashIndexValueSpace(transactionProvider, fileIndex);

        space.index = HashIndexSpace.create(indexManager, configuration, transactionProvider, fileAllocator,
                fileAllocator.allocateFile(transaction), filePrefix, fixedKey, maxKeySize, false, maxValueSize,
                keyNormalizer, space.new IndexValueConverter(),
                space.new IndexValueListener(), properties);

        space.writeHeader(space.index.getFileIndex());

        return space;
    }

    public static <K> HashIndexValueSpace open(IndexManager indexManager, IndexSchemaConfiguration configuration,
                                               ITransactionProvider transactionProvider, IDataFileAllocator fileAllocator, int fileIndex,
                                               String filePrefix, boolean fixedKey, int maxKeySize, int maxValueSize,
                                               IKeyNormalizer<K> keyNormalizer, Map<String, String> properties) {
        Assert.notNull(transactionProvider);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        bindFile(transaction, fileIndex, filePrefix + "-values", configuration.getPathIndex());

        HashIndexValueSpace space = new HashIndexValueSpace(transactionProvider, fileIndex);
        int indexFileIndex = space.readHeader();

        space.index = HashIndexSpace.open(indexManager, configuration, transactionProvider, fileAllocator, indexFileIndex,
                filePrefix, fixedKey, maxKeySize, false, maxValueSize, keyNormalizer, space.new IndexValueConverter(),
                space.new IndexValueListener(), properties);

        return space;
    }

    @Override
    public IUniqueIndex<K, IIndexValue> getIndex() {
        return index;
    }

    @Override
    public IIndexValue find(K key, boolean readOnly) {
        IRawReadRegion region = index.findValueRegion(key, readOnly);
        if (region != null)
            return IndexValue.open(this, region, readOnly);
        else
            return null;
    }

    private HashIndexValueSpace(ITransactionProvider transactionProvider, int fileIndex) {
        super(transactionProvider, fileIndex);
    }
}
