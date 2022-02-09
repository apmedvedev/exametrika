/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.ops;

import java.io.Serializable;
import java.util.List;

import com.exametrika.api.exadb.core.BatchOperation;
import com.exametrika.api.exadb.core.IBatchControl;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.index.IIndexManager.IndexInfo;
import com.exametrika.api.exadb.index.IIndexOperationManager;
import com.exametrika.api.exadb.index.ISortedIndex;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link IndexOperationManager} represents an index operation manager.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class IndexOperationManager implements IIndexOperationManager {
    private final IDatabaseContext context;

    public IndexOperationManager(IDatabaseContext context) {
        Assert.notNull(context);

        this.context = context;
    }

    @Override
    public void rebuildStatistics(double keyRatio, long rebuildThreshold, boolean force) {
        context.getDatabase().transaction(new RebuildStatisticsOperation(keyRatio, rebuildThreshold, force));
    }

    private static class RebuildStatisticsOperation extends BatchOperation implements Serializable {
        private final double keyRatio;
        private final long rebuildThreshold;
        private final boolean force;
        private int indexId = -1;
        private int indexPos = -1;
        private Pair<ByteArray, Long> indexBin;
        private transient ISortedIndex index;
        private transient IIndexManager indexManager;

        public RebuildStatisticsOperation(double keyRatio, long rebuildThreshold, boolean force) {
            this.keyRatio = keyRatio;
            this.rebuildThreshold = rebuildThreshold;
            this.force = force;
        }

        @Override
        public boolean run(ITransaction transaction, IBatchControl batchControl) {
            ensureIndexManager(transaction);

            batchControl.setPageCachingEnabled(false);
            batchControl.setNonCachedPagesInvalidationQueueSize(10);

            while (true) {
                if (indexId == -1 || indexManager.findIndex(indexId) == null) {
                    index = null;
                    indexId = -1;
                    indexBin = null;

                    List<IndexInfo> indexes = indexManager.getIndexes();
                    if (indexPos == -1) {
                        if (indexes.isEmpty())
                            return true;

                        indexPos = 0;
                        indexId = indexes.get(0).id;
                    } else {
                        if (indexes.size() >= indexPos + 1)
                            return true;

                        indexPos++;
                        indexId = indexes.get(indexPos).id;
                    }
                }

                if (index == null || index.isStale())
                    index = indexManager.getIndex(indexId);

                indexBin = index.rebuildStatistics(batchControl, indexBin, keyRatio, rebuildThreshold, force);
                if (indexBin != null)
                    return false;

                index = null;
                indexId = -1;

                if (!batchControl.canContinue())
                    return false;
            }
        }

        private void ensureIndexManager(ITransaction transaction) {
            if (indexManager != null)
                return;

            indexManager = transaction.findExtension(IIndexManager.NAME);
        }
    }

    ;
}
