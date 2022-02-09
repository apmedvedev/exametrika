/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.nodes;

import com.exametrika.api.aggregator.nodes.ILogRootNode;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.spi.exadb.objectdb.fields.IBlobStoreField;


/**
 * The {@link LogRootNode} is a log root node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class LogRootNode extends RootNode implements ILogRootNode {
    protected static final int BLOB_STORE_FIELD = 7;

    public LogRootNode(INode node) {
        super(node);
    }

    @Override
    public long getBlobStoreFreeSpace() {
        IBlobStoreField blobStore = getField(BLOB_STORE_FIELD);
        return blobStore.getFreeSpace();
    }
}