/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.nodes;


/**
 * The {@link INodeComponentVersion} represents a node component version node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface INodeComponentVersion extends IAgentComponentVersion {
    /**
     * Returns host.
     *
     * @return host
     */
    IHostComponent getHost();

    /**
     * Returns node transactions.
     *
     * @return node transactions
     */
    Iterable<ITransactionComponent> getTransactions();
}
