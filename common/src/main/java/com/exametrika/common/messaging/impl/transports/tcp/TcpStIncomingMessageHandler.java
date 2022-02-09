/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.transports.tcp;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.utils.Assert;

/**
 * The {@link TcpStIncomingMessageHandler} represents a single-threaded handler of incoming messages.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class TcpStIncomingMessageHandler implements ITcpIncomingMessageHandler {
    private final IReceiver receiver;
    private final ISerializationRegistry serializationRegistry;

    public TcpStIncomingMessageHandler(IReceiver receiver, ISerializationRegistry serializationRegistry) {
        Assert.notNull(receiver);
        Assert.notNull(serializationRegistry);

        this.receiver = receiver;
        this.serializationRegistry = serializationRegistry;
    }

    public IReceiver getReceiver() {
        return receiver;
    }

    public ISerializationRegistry getSerializationRegistry() {
        return serializationRegistry;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
}