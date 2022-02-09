/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.nodes;

import com.exametrika.common.utils.Assert;

/**
 * The {@link Dependency} represents a dependency between derived stack name node and stack node.
 *
 * @param <T> dependency node type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public class Dependency<T extends IAggregationNode> {
    private final T node;
    private final boolean total;

    public Dependency(T node, boolean total) {
        Assert.notNull(node);

        this.node = node;
        this.total = total;
    }

    public T getNode() {
        return node;
    }

    public boolean isTotal() {
        return total;
    }

    @Override
    public String toString() {
        return node.toString();
    }
}