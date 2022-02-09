/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.nodes;


/**
 * The {@link IBackgroundRootNode} represents a background root stack node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IBackgroundRootNode extends IEntryPointNode {
    /**
     * Returns anomalies.
     *
     * @return anomalies
     */
    IStackLogNode getAnomalies();
}
