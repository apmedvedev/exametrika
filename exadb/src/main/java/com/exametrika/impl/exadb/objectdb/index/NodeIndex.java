/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.index;

import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link NodeIndex} implements {@link INodeIndex}.
 *
 * @param <K> key type
 * @param <V> node type
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class NodeIndex<K, V> implements INodeIndex<K, V> {
    protected final IDatabaseContext context;
    private IUniqueIndex<Object, Long> index;

    public NodeIndex(IDatabaseContext context, IUniqueIndex<Object, Long> index) {
        Assert.notNull(context);
        Assert.notNull(index);

        this.context = context;
        this.index = index;
    }

    public IUniqueIndex<Object, Long> getIndex() {
        if (!index.isStale())
            return index;
        else {
            index = refreshIndex(index.getId());
            return index;
        }
    }

    @Override
    public boolean contains(K key) {
        Object indexKey = getKey(key);

        V value = findInCache(indexKey);
        if (value != null)
            return true;

        Long id = getIndex().find(indexKey);
        if (id != null)
            return true;
        else
            return false;
    }

    @Override
    public V find(K key) {
        Object indexKey = getKey(key);

        V value = findInCache(indexKey);
        if (value != null)
            return value;

        Long id = getIndex().find(indexKey);
        if (id != null)
            return findById(id);
        else
            return null;
    }

    public void add(K key, Object value, boolean updateIndex, boolean updateCache) {
        if (key == null)
            return;

        Object indexKey = getKey(key);

        if (updateIndex)
            getIndex().add(indexKey, getId(value));
        if (updateCache)
            addToCache(indexKey, value);
    }

    public void update(K oldKey, K newKey, Object value) {
        remove(oldKey, value, true, true);
        add(newKey, value, true, true);
    }

    public void remove(K key, Object value, boolean updateIndex, boolean updateCache) {
        if (key == null)
            return;

        Object indexKey = getKey(key);

        if (updateIndex)
            getIndex().remove(indexKey);
        if (updateCache)
            removeFromCache(indexKey);
    }

    public void unload() {
        index.unload();
    }

    protected V findInCache(Object indexKey) {
        return null;
    }

    protected void addToCache(Object indexKey, Object value) {
    }

    protected void removeFromCache(Object indexKey) {
    }

    protected abstract Object getKey(K key);

    protected abstract long getId(Object value);

    protected abstract V findById(long id);

    protected abstract IUniqueIndex<Object, Long> refreshIndex(int id);
}
