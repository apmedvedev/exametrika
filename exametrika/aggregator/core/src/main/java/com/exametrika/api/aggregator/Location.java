/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator;


/**
 * The {@link Location} represents a location of period node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Location {
    private final long scopeId;
    private final long metricId;

    public Location(long scopeId, long metricId) {
        this.scopeId = scopeId;
        this.metricId = metricId;
    }

    public long getScopeId() {
        return scopeId;
    }

    public long getMetricId() {
        return metricId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Location))
            return false;

        Location configuration = (Location) o;
        return scopeId == configuration.scopeId && metricId == configuration.metricId;
    }

    @Override
    public int hashCode() {
        return 31 * (int) (scopeId ^ (scopeId >>> 32)) + (int) (metricId ^ (metricId >>> 32));
    }

    @Override
    public String toString() {
        return Long.toString(scopeId) + ":" + Long.toString(metricId);
    }
}
