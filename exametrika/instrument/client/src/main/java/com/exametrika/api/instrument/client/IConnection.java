/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.client;


/**
 * The {@link IConnection} is a connection to instrumentation agent.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IConnection {
    /**
     * Is instrumentation started?
     *
     * @return true if instrumentation is started
     */
    boolean isStarted();

    /**
     * Starts instrumentation.
     *
     * @param configurationPath path to instrumentation configuration
     */
    void start(String configurationPath);

    /**
     * Stops instrumentation.
     */
    void stop();
}
