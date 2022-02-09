/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;


/**
 * The {@link IDeliveryHandler} is used for confirmation of actual message delivery by application.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IDeliveryHandler {
    /**
     * Called by application when specified message has been delivered.
     *
     * @param message delivered message
     */
    void onDelivered(IMessage message);
}