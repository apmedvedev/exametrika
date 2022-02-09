/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.index;

import java.util.HashMap;
import java.util.Map;

import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link NodeCachedIndex} implements {@link INodeIndex}.
 *
 * @param <K> key type
 * @param <V> node type
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class NodeCachedIndex<K, V> extends NodeIndex<K, V> {
    private final Map<Object, INode> nodes;

    public NodeCachedIndex(IDatabaseContext context, IUniqueIndex<Object, Long> index, boolean cached) {
        super(context, index);

        if (cached)
            nodes = new HashMap<Object, INode>();
        else
            nodes = null;
    }

    @Override
    protected V findInCache(Object indexKey) {
        if (nodes != null) {
            INode node = nodes.get(indexKey);
            if (node != null) {
                ((Node) node).refresh();
                return node.getObject();
            }
        }

        return null;
    }

    @Override
    protected void addToCache(Object indexKey, Object value) {
        if (nodes != null)
            nodes.put(indexKey, (INode) value);
    }

    @Override
    protected void removeFromCache(Object indexKey) {
        if (nodes != null)
            nodes.remove(indexKey);
    }

    @Override
    protected long getId(Object value) {
        return ((INode) value).getId();
    }
}
