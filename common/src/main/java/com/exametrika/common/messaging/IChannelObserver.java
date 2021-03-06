/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;


/**
 * The {@link IChannelObserver} represents an observer of channel events.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IChannelObserver {
    /**
     * Adds channel listener.
     *
     * @param listener listener
     */
    void addChannelListener(IChannelListener listener);

    /**
     * Removes channel listener.
     *
     * @param listener listener
     */
    void removeChannelListener(IChannelListener listener);

    /**
     * Removes all channel listeners.
     */
    void removeAllChannelListeners();
}
