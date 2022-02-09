/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.index;

import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.index.ISortedIndex;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ObjectNodeSortedIndex} implements {@link INodeIndex}.
 *
 * @param <K> key type
 * @param <V> node type
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ObjectNodeSortedIndex<K, V> extends NodeSortedIndex<K, V> {
    private final ObjectSpace space;

    public ObjectNodeSortedIndex(IDatabaseContext context, ISortedIndex<Object, Long> index, ObjectSpace space) {
        super(context, index);

        Assert.notNull(space);

        this.space = space;
    }

    @Override
    protected V findById(long id) {
        return space.findNodeById(id);
    }

    @Override
    protected Object getKey(K key) {
        return key;
    }

    @Override
    protected long getId(Object value) {
        return ((INode) value).getId();
    }

    @Override
    protected IUniqueIndex<Object, Long> refreshIndex(int id) {
        IIndexManager indexManager = context.findTransactionExtension(IIndexManager.NAME);
        return indexManager.getIndex(id);
    }
}
