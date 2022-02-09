/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.fields;


/**
 * The {@link VersionTime} represents a location of period node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class VersionTime {
    private final long nodeId;
    private final long time;

    public VersionTime(long nodeId, long time) {
        this.nodeId = nodeId;
        this.time = time;
    }

    public long getNodeId() {
        return nodeId;
    }

    public long getTime() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof VersionTime))
            return false;

        VersionTime configuration = (VersionTime) o;
        return nodeId == configuration.nodeId && time == configuration.time;
    }

    @Override
    public int hashCode() {
        return 31 * (int) (nodeId ^ (nodeId >>> 32)) + (int) (time ^ (time >>> 32));
    }

    @Override
    public String toString() {
        return Long.toString(nodeId) + ":" + Long.toString(time);
    }
}
