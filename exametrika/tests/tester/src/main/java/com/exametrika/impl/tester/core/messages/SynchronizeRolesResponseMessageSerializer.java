/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.messages;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link SynchronizeRolesResponseMessageSerializer} is a serializer for {@link SynchronizeRolesResponseMessage}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SynchronizeRolesResponseMessageSerializer extends AbstractSerializer {
    public static final UUID ID = UUID.fromString("8152ec9e-af6d-4b0c-bcdc-84d261afb711");

    public SynchronizeRolesResponseMessageSerializer() {
        super(ID, SynchronizeRolesResponseMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        SynchronizeRolesResponseMessage part = (SynchronizeRolesResponseMessage) object;

        serialization.writeInt(part.getRolesHashes().size());
        for (Map.Entry<String, String> entry : part.getRolesHashes().entrySet()) {
            serialization.writeString(entry.getKey());
            serialization.writeString(entry.getValue());
        }
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        int count = deserialization.readInt();
        Map<String, String> rolesHashes = new LinkedHashMap<String, String>(count);
        for (int i = 0; i < count; i++)
            rolesHashes.put(deserialization.readString(), deserialization.readString());

        return new SynchronizeRolesResponseMessage(rolesHashes);
    }
}
