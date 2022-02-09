/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.exa.server.nodes;

import com.exametrika.api.component.nodes.IAgentComponent;
import com.exametrika.api.component.nodes.IHealthComponentVersion;


/**
 * The {@link IExaAgentComponentVersion} represents a exa agent component version node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IExaAgentComponentVersion extends IHealthComponentVersion {
    /**
     * Returns node.
     *
     * @return node
     */
    IAgentComponent getParent();

    /**
     * Returns server.
     *
     * @return server
     */
    IExaServerComponent getServer();
}
