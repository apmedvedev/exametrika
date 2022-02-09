/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.nodes;


/**
 * The {@link IPrimaryEntryPointNode} represents a primary entry point node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IPrimaryEntryPointNode extends IEntryPointNode {
    /**
     * Returns transaction failures and their dependencies.
     *
     * @return transaction failures node
     */
    IStackLogNode getTransactionFailures();

    /**
     * Returns transaction anomalies.
     *
     * @return transaction anomalies
     */
    IStackLogNode getAnomalies();
}
