/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.messages;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.impl.aggregator.common.model.SerializeNameDictionary.SerializeNameId;

/**
 * The {@link RemoveNamesMessageSerializer} is a serializer for {@link RemoveNamesMessage}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class RemoveNamesMessageSerializer extends AbstractSerializer {
    public static final UUID ID = UUID.fromString("055be599-0079-4634-bc76-b0b75614550e");

    public RemoveNamesMessageSerializer() {
        super(ID, RemoveNamesMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        RemoveNamesMessage part = (RemoveNamesMessage) object;

        serialization.writeInt(part.getRemovedNames().size());
        for (int i = 0; i < part.getRemovedNames().size(); i++) {
            SerializeNameId id = part.getRemovedNames().get(i);
            serialization.writeByte(id.type);
            serialization.writeLong(id.id);
        }
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        int count = deserialization.readInt();
        List<SerializeNameId> ids = new ArrayList<SerializeNameId>(count);
        for (int i = 0; i < count; i++) {
            byte type = deserialization.readByte();
            long nameId = deserialization.readLong();

            ids.add(new SerializeNameId(type, nameId));
        }

        return new RemoveNamesMessage(ids);
    }
}
