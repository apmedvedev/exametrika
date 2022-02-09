/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.jvm;


/**
 * The {@link IJmsConsumerRawRequest} represents a jms consumer raw request.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IJmsConsumerRawRequest {
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

    /**
     * Returns message's property.
     *
     * @param name property name
     * @return property value
     */
    Object getProperty(String name);
}
