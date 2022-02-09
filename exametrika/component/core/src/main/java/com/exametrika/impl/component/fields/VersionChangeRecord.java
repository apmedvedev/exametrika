/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.fields;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.component.IVersionChangeRecord;


/**
 * The {@link VersionChangeRecord} is a version change record.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class VersionChangeRecord implements IVersionChangeRecord {
    private final int nodeSchemaIndex;
    private final long time;
    private final Type type;
    private final long scopeId;
    private final long groupScopeId;
    private final long nodeId;
    private final long prevVersionNodeId;

    public VersionChangeRecord(int nodeSchemaIndex, long time, Type type, long scopeId, long groupScopeId, long nodeId, long prevVersionNodeId) {
        Assert.notNull(type);

        this.nodeSchemaIndex = nodeSchemaIndex;
        this.time = time;
        this.type = type;
        this.scopeId = scopeId;
        this.groupScopeId = groupScopeId;
        this.nodeId = nodeId;
        this.prevVersionNodeId = prevVersionNodeId;
    }

    @Override
    public int getNodeSchemaIndex() {
        return nodeSchemaIndex;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public long getScopeId() {
        return scopeId;
    }

    @Override
    public long getGroupScopeId() {
        return groupScopeId;
    }

    @Override
    public long getNodeId() {
        return nodeId;
    }

    @Override
    public long getPreviousVersionNodeId() {
        return prevVersionNodeId;
    }
}
