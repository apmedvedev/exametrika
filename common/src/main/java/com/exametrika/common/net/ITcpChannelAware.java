/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;


/**
 * The {@link ITcpChannelAware} is used to assign channel after creation to data object associated with channel.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITcpChannelAware {
    /**
     * Sets created channel.
     *
     * @param channel channel
     */
    void setChannel(ITcpChannel channel);
}
