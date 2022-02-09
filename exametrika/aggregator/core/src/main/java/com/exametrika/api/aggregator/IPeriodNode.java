/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator;

import com.exametrika.api.aggregator.common.model.IMetricLocation;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.schema.IPeriodNodeSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.common.utils.Pair;


/**
 * The {@link IPeriodNode} represents a periodic node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IPeriodNode extends INode {
    @Override
    IPeriodNodeSchema getSchema();

    /**
     * Returns node space.
     *
     * @return node space
     */
    @Override
    IPeriodSpace getSpace();

    /**
     * Returns node period.
     *
     * @return node period
     */
    IPeriod getPeriod();

    /**
     * Returns node location.
     *
     * @return node location
     */
    Location getLocation();

    /**
     * Returns node scope.
     *
     * @return node scope
     */
    IScopeName getScope();

    /**
     * Returns node metric location.
     *
     * @return node metric location
     */
    IMetricLocation getMetric();

    /**
     * Return node with current node's scope name and metric location in previous period.
     *
     * @return node with current node's scope name and metric location in previous period or null if previous period does not have such node
     */
    <T> T getPreviousPeriodNode();

    /**
     * Returns iterator over nodes with current node's scope name and metric location in previous periods starting from current period.
     * Period node is null for those periods where node with current node's scope name and metric location does not exist.
     *
     * @return iterator over nodes with same locators of previous periods starting from current period
     */
    <T> Iterable<Pair<IPeriod, T>> getPeriodNodes();
}
