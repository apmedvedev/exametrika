/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import com.exametrika.common.net.ITcpChannelReader;


/**
 * The {@link ITcpReceiveQueue} is a TCP receive queue.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITcpReceiveQueue extends ITcpChannelReader {
    /**
     * Locks flow of messages.
     */
    void lockFlow();

    /**
     * Unlocks flow of messages.
     */
    void unlockFlow();
}
