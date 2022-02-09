/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.messages;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.tester.core.messages.ControlMessage.Type;

/**
 * The {@link ControlMessageSerializer} is a serializer for {@link ControlMessage}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ControlMessageSerializer extends AbstractSerializer {
    public static final UUID ID = UUID.fromString("f9b8ff65-ff07-4abc-8c9b-638c608d8b17");

    public ControlMessageSerializer() {
        super(ID, ControlMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        ControlMessage part = (ControlMessage) object;
        Assert.notNull(part);

        serialization.writeString(part.getNodeName());
        Serializers.writeEnum(serialization, part.getType());
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        String nodeName = deserialization.readString();
        Type type = Serializers.readEnum(deserialization, Type.class);
        return new ControlMessage(nodeName, type);
    }
}
