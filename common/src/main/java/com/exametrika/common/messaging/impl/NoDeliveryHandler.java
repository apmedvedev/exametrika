/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl;

import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IMessage;

/**
 * The {@link NoDeliveryHandler} represents a no delivery handler.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NoDeliveryHandler implements IDeliveryHandler {
    @Override
    public void onDelivered(IMessage message) {
    }
}
