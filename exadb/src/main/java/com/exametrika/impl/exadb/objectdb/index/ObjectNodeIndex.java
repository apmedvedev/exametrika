/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.index;

import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ObjectNodeIndex} implements {@link INodeIndex}.
 *
 * @param <K> key type
 * @param <V> node type
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ObjectNodeIndex<K, V> extends NodeCachedIndex<K, V> {
    private final ObjectSpace space;

    public ObjectNodeIndex(IDatabaseContext context, IUniqueIndex<Object, Long> index, ObjectSpace space, boolean cached) {
        super(context, index, cached);

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
    protected IUniqueIndex<Object, Long> refreshIndex(int id) {
        IIndexManager indexManager = context.findTransactionExtension(IIndexManager.NAME);
        return indexManager.getIndex(id);
    }
}
