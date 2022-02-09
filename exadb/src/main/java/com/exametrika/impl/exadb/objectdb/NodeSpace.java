/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb;

import java.util.Map;

import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeSpace;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.impl.exadb.objectdb.cache.NodeCache;
import com.exametrika.impl.exadb.objectdb.cache.NodeCacheManager;
import com.exametrika.impl.exadb.objectdb.cache.NodeManager;
import com.exametrika.impl.exadb.objectdb.index.NodeFullTextIndex;
import com.exametrika.impl.exadb.objectdb.schema.StructuredBlobFieldSchema;
import com.exametrika.spi.exadb.core.ITransactionProvider;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link NodeSpace} is an abstract node space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class NodeSpace implements INodeSpace {
    protected final int fileIndex;
    protected final ITransactionProvider transactionProvider;
    protected final String filesPath;
    protected final String indexesPath;
    protected final String filePrefix;

    public NodeSpace(ITransactionProvider transactionProvider, int fileIndex, String filePrefix) {
        Assert.notNull(transactionProvider);
        Assert.isTrue(fileIndex != 0);
        Assert.notNull(filePrefix);

        this.transactionProvider = transactionProvider;
        this.fileIndex = fileIndex;
        this.filePrefix = filePrefix;
        this.filesPath = Spaces.getSpaceFilesDirName(filePrefix);
        this.indexesPath = Spaces.getSpaceIndexesDirName(filePrefix);
    }

    public final IRawTransaction getRawTransaction() {
        return transactionProvider.getRawTransaction();
    }

    @Override
    public final ITransaction getTransaction() {
        return transactionProvider.getTransaction();
    }

    public abstract NodeManager getNodeManager();

    public abstract NodeCacheManager getNodeCacheManager();

    @Override
    public abstract NodeCache getNodeCache();

    public final String getFilesPath() {
        return filesPath;
    }

    public final String getIndexesPath() {
        return indexesPath;
    }

    public abstract boolean isClosed();

    public abstract void addIndexValue(IFieldSchema field, Object key, INode node, boolean updateIndex, boolean updateCache);

    public abstract void updateIndexValue(IFieldSchema field, Object oldKey, Object newKey, INode node);

    public abstract void removeIndexValue(IFieldSchema field, Object key, INode node, boolean updateIndex, boolean updateCache);

    public abstract NodeFullTextIndex getFullTextIndex();

    public abstract Map<String, String> getProperties(NodeSchemaConfiguration configuration);

    public abstract String getFieldId(IField field);

    public abstract IUniqueIndex getBlobIndex(StructuredBlobFieldSchema field, int blobIndex);
}
