/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.messages;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link ActionMessageSerializer} is a serializer for {@link ActionMessage}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ActionMessageSerializer extends AbstractSerializer {
    public static final UUID ID = UUID.fromString("2bc44d6d-ab50-4dc4-a875-caf7694f5585");

    public ActionMessageSerializer() {
        super(ID, ActionMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        ActionMessage part = (ActionMessage) object;

        serialization.writeLong(part.getActionId());
        serialization.writeObject(part.getAction());
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        long actionId = deserialization.readLong();
        Object action = deserialization.readObject();

        return new ActionMessage(actionId, action);
    }
}
