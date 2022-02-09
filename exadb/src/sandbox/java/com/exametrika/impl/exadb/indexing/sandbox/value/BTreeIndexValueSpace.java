/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.value;

import java.util.Map;

import com.exametrika.api.exadb.index.IBTreeIndex;
import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.index.IndexManager;
import com.exametrika.impl.exadb.index.btree.BTreeIndexSpace;
import com.exametrika.impl.exadb.indexing.sandbox.IIndexValue;
import com.exametrika.spi.exadb.core.IDataFileAllocator;
import com.exametrika.spi.exadb.core.ITransactionProvider;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;


/**
 * The {@link BTreeIndexValueSpace} is a key-value space based on BTreeIndex.
 *
 * @param <K> key type
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class BTreeIndexValueSpace<K> extends IndexValueSpace<K> {
    private BTreeIndexSpace index;

    public static <K> BTreeIndexValueSpace create(IndexManager indexManager, IndexSchemaConfiguration configuration,
                                                  ITransactionProvider transactionProvider, IDataFileAllocator fileAllocator,
                                                  int fileIndex, String filePrefix, boolean fixedKey, int maxKeySize, int maxValueSize,
                                                  IKeyNormalizer<K> keyNormalizer, boolean createStatistics, Map<String, String> properties) {
        Assert.notNull(transactionProvider);

        IRawTransaction transaction = transactionProvider.getRawTransaction();

        bindFile(transaction, fileIndex, filePrefix + "-values", configuration.getPathIndex());

        BTreeIndexValueSpace space = new BTreeIndexValueSpace(transactionProvider, fileIndex);

        space.index = BTreeIndexSpace.create(indexManager, configuration, transactionProvider, fileAllocator,
                fileAllocator.allocateFile(transaction), filePrefix, fixedKey, maxKeySize, false, maxValueSize,
                keyNormalizer, space.new IndexValueConverter(), createStatistics, space.new IndexValueListener(), properties, false);

        space.writeHeader(space.index.getFileIndex());

        return space;
    }

    public static <K> BTreeIndexValueSpace open(IndexManager indexManager, IndexSchemaConfiguration configuration,
                                                ITransactionProvider transactionProvider, int fileIndex, String filePrefix, boolean fixedKey, int maxKeySize,
                                                int maxValueSize, IKeyNormalizer<K> keyNormalizer, Map<String, String> properties) {
        Assert.notNull(transactionProvider);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        bindFile(transaction, fileIndex, filePrefix + "-values", configuration.getPathIndex());

        BTreeIndexValueSpace space = new BTreeIndexValueSpace(transactionProvider, fileIndex);
        int indexFileIndex = space.readHeader();

        space.index = BTreeIndexSpace.open(indexManager, configuration, transactionProvider, indexFileIndex,
                filePrefix, fixedKey, maxKeySize, false, maxValueSize, keyNormalizer, space.new IndexValueConverter(),
                space.new IndexValueListener(), properties, true);

        return space;
    }

    @Override
    public IBTreeIndex<K, IIndexValue> getIndex() {
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

    private BTreeIndexValueSpace(ITransactionProvider transactionProvider, int fileIndex) {
        super(transactionProvider, fileIndex);
    }
}
