/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.value;

import java.util.Map;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.ITreeIndex;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.index.IndexManager;
import com.exametrika.impl.exadb.index.memory.TreeIndexSpace;
import com.exametrika.impl.exadb.indexing.sandbox.IIndexValue;
import com.exametrika.spi.exadb.core.IDataFileAllocator;
import com.exametrika.spi.exadb.core.ITransactionProvider;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;


/**
 * The {@link TreeIndexValueSpace} is a key-value space based on TreeIndex.
 *
 * @param <K> key type
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class TreeIndexValueSpace<K> extends IndexValueSpace<K> {
    private TreeIndexSpace index;

    public static <K> TreeIndexValueSpace create(IndexManager indexManager, IndexSchemaConfiguration configuration,
                                                 ITransactionProvider transactionProvider, IDataFileAllocator fileAllocator,
                                                 int fileIndex, String filePrefix, boolean fixedKey, int maxKeySize, boolean fixedValue, int maxValueSize,
                                                 IKeyNormalizer<K> keyNormalizer, boolean createStatistics, Map<String, String> properties) {
        Assert.notNull(transactionProvider);

        IRawTransaction transaction = transactionProvider.getRawTransaction();

        bindFile(transaction, fileIndex, filePrefix + "-values", configuration.getPathIndex());

        TreeIndexValueSpace space = new TreeIndexValueSpace(transactionProvider, fileIndex);

        space.index = TreeIndexSpace.create(indexManager, configuration, transactionProvider, fileAllocator,
                fileAllocator.allocateFile(transaction), filePrefix, fixedKey, maxKeySize, false, maxValueSize,
                keyNormalizer, space.new IndexValueConverter(), createStatistics,
                space.new IndexValueListener(), properties);

        space.writeHeader(space.index.getFileIndex());

        return space;
    }

    public static <K> TreeIndexValueSpace open(IndexManager indexManager, IndexSchemaConfiguration configuration,
                                               ITransactionProvider transactionProvider, IDataFileAllocator fileAllocator, int fileIndex,
                                               String filePrefix, boolean fixedKey, int maxKeySize, boolean fixedValue, int maxValueSize, IKeyNormalizer<K> keyNormalizer,
                                               Map<String, String> properties) {
        Assert.notNull(transactionProvider);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        bindFile(transaction, fileIndex, filePrefix + "-values", configuration.getPathIndex());

        TreeIndexValueSpace space = new TreeIndexValueSpace(transactionProvider, fileIndex);
        int indexFileIndex = space.readHeader();

        space.index = TreeIndexSpace.open(indexManager, configuration, transactionProvider, fileAllocator, indexFileIndex,
                filePrefix, fixedKey, maxKeySize, false, maxValueSize, keyNormalizer, space.new IndexValueConverter(),
                space.new IndexValueListener(), properties, true);

        return space;
    }

    @Override
    public ITreeIndex<K, IIndexValue> getIndex() {
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

    private TreeIndexValueSpace(ITransactionProvider transactionProvider, int fileIndex) {
        super(transactionProvider, fileIndex);
    }
}
