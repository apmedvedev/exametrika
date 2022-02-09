/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.jvm;


/**
 * The {@link IUdpRawRequest} represents a udp raw request.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IUdpRawRequest {
    /**
     * Returns host and port.
     *
     * @return host and port
     */
    String getHostPort();

    /**
     * Is request receive or send request
     *
     * @return true if request is receive request
     */
    boolean isReceive();
}
