/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.messages;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link TimeSynchronizationMessageSerializer} is a serializer for {@link TimeSynchronizationMessage}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TimeSynchronizationMessageSerializer extends AbstractSerializer {
    public static final UUID ID = UUID.fromString("aecf4a58-3e5a-4b69-ac5e-734be0a9aaf4");

    public TimeSynchronizationMessageSerializer() {
        super(ID, TimeSynchronizationMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        TimeSynchronizationMessage part = (TimeSynchronizationMessage) object;

        serialization.writeLong(part.getAgentTime());
        serialization.writeLong(part.getServerTime());
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        long agentTime = deserialization.readLong();
        long serverTime = deserialization.readLong();

        return new TimeSynchronizationMessage(agentTime, serverTime);
    }
}
