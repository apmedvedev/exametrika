/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;


/**
 * The {@link IAgentFailureDetector} represents a detector of agent failures.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IAgentFailureDetector {
    String NAME = "agentFailureDetector";

    /**
     * Is agent with specified identifier active?
     *
     * @param agentId agent identifier
     * @return true if agent is active
     */
    boolean isActive(String agentId);

    /**
     * Adds failure listener.
     *
     * @param listener listener
     */
    void addFailureListener(IAgentFailureListener listener);

    /**
     * Removes failure listener.
     *
     * @param listener listener
     */
    void removeFailureListener(IAgentFailureListener listener);
}
