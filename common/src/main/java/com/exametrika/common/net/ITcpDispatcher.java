/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;


/**
 * The {@link ITcpDispatcher} is a central dispatcher of {@link ITcpServer}s and {@link ITcpChannel}s.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITcpDispatcher {
    /**
     * Returns number of servers.
     *
     * @return number of servers
     */
    int getServerCount();

    /**
     * Returns number of channels.
     *
     * @return number of channels
     */
    int getChannelCount();
}
