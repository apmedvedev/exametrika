/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.jvm;


/**
 * The {@link IJmsProducerRawRequest} represents a jms producer raw request.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IJmsProducerRawRequest {
    /**
     * Returns destination type.
     *
     * @return destination type
     */
    String getDestinationType();

    /**
     * Returns destination name.
     *
     * @return destination name
     */
    String getDestinationName();
}
