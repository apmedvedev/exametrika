/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;


/**
 * The {@link IAgentFailureListener} represents a listener of agent failure events.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IAgentFailureListener {
    /**
     * Called when agent with specified identifier has been activated.
     *
     * @param agentId agent identifier
     */
    void onAgentActivated(String agentId);

    /**
     * Called when agent with specified identifier has been failed.
     *
     * @param agentId agent identifier
     */
    void onAgentFailed(String agentId);
}
