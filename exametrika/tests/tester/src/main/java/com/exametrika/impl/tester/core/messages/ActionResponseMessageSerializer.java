/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.messages;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link ActionResponseMessageSerializer} is a serializer for {@link ActionResponseMessage}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ActionResponseMessageSerializer extends AbstractSerializer {
    public static final UUID ID = UUID.fromString("ab28394c-7f6b-47c1-9b23-9f6f92d1498");

    public ActionResponseMessageSerializer() {
        super(ID, ActionResponseMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        ActionResponseMessage part = (ActionResponseMessage) object;

        serialization.writeString(part.getNodeName());
        serialization.writeString(part.getActionName());
        serialization.writeObject(part.getError());
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        String nodeName = deserialization.readString();
        String actionName = deserialization.readString();
        Throwable error = deserialization.readObject();

        return new ActionResponseMessage(nodeName, actionName, error);
    }
}
