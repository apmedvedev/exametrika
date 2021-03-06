/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl.message;

import java.util.List;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Immutables;


/**
 * The {@link SerializedMessagePart} is a mesage part in serialized form that is used to deserialize message part on-demand.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SerializedMessagePart implements IMessagePart {
    private final ISerializationRegistry serializationRegistry;
    private final List<ByteArray> buffers;
    private final int size;
    private final IMessagePart originalMessagePart;

    /**
     * Creates a new object.
     *
     * @param serializationRegistry serialization registry
     * @param buffers               data buffers of serialized message part
     * @param size                  size of data buffers
     * @param originalMessagePart   original message part or null if not set
     */
    public SerializedMessagePart(ISerializationRegistry serializationRegistry, List<ByteArray> buffers, int size, IMessagePart originalMessagePart) {
        Assert.notNull(serializationRegistry);
        Assert.notNull(buffers);

        this.serializationRegistry = serializationRegistry;
        this.buffers = Immutables.wrap(buffers);
        this.size = size;
        this.originalMessagePart = originalMessagePart;
    }

    /**
     * Returns serialization registry to deserialize message part.
     *
     * @return serialization registry
     */
    public ISerializationRegistry getRegistry() {
        return serializationRegistry;
    }

    /**
     * Returns data buffers of serialized message part.
     *
     * @return data buffers of serialized message part
     */
    public List<ByteArray> getBuffers() {
        return buffers;
    }

    @Override
    public int getSize() {
        return size;
    }

    public IMessagePart getMessagePart() {
        return originalMessagePart;
    }
}
