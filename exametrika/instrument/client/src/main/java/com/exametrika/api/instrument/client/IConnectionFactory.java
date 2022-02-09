/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.client;

import java.util.List;


/**
 * The {@link IConnectionFactory} is a factory for instrumentation agent connections.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IConnectionFactory {
    /**
     * Returns list of processes avaliable for instrumentation.
     *
     * @return list of processes avaliable for instrumentation
     */
    List<ProcessInfo> getAvailableProcesses();

    /**
     * Creates connection to instrumentation agent in specified process loading the agent if it is not loaded yet.
     *
     * @param pid       identifier of instrumentated process
     * @param agentPath path to instrumentation agent library
     * @return connection to instrumentation agent
     */
    IConnection connect(String pid, String agentPath);

    /**
     * Creates connection to instrumentation agent in specified process.
     *
     * @param pid identifier of instrumentated process
     * @return connection to instrumentation agent or null if agent is not loaded
     */
    IConnection connect(String pid);
}
