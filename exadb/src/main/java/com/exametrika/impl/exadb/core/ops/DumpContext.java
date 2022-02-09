/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.ops;

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;


/**
 * The {@link DumpContext} is a context of dump operation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class DumpContext implements IDumpContext {
    private final int flags;
    private final JsonObject query;
    private final TLongSet traversedNodes = new TLongHashSet(10, 0.5f, Long.MAX_VALUE);

    public DumpContext(int flags, JsonObject query) {
        Assert.notNull(flags);

        this.flags = flags;
        this.query = query;
    }

    @Override
    public int getFlags() {
        return flags;
    }

    @Override
    public JsonObject getQuery() {
        return query;
    }

    @Override
    public boolean isNodeTraversed(long nodeId) {
        return traversedNodes.contains(nodeId);
    }

    public void traverseNode(long nodeId) {
        traversedNodes.add(nodeId);
    }

    public void reset() {
        traversedNodes.clear();
    }
}
