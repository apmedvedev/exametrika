/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl;

import java.util.List;

import com.exametrika.common.messaging.IDeliveryHandler;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;

/**
 * The {@link CompositeDeliveryHandler} represents a composite delivery handler.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CompositeDeliveryHandler implements IDeliveryHandler {
    private final List<IDeliveryHandler> handlers;

    public CompositeDeliveryHandler(List<IDeliveryHandler> handlers) {
        Assert.notNull(handlers);

        this.handlers = handlers;
    }

    @Override
    public void onDelivered(IMessage message) {
        for (IDeliveryHandler handler : handlers)
            handler.onDelivered(message);
    }
}
