/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;


/**
 * The {@link ITcpChannelReader} listens to TCP channel read events.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITcpChannelReader {
    /**
     * Can reader read from specified channel?
     *
     * @param channel channel to read
     * @return if true - reader can read from specified channel, if false - reader can not read from specified channel
     */
    boolean canRead(ITcpChannel channel);

    /**
     * Called when channel can read data.
     *
     * @param channel channel to read
     */
    void onRead(ITcpChannel channel);
}
