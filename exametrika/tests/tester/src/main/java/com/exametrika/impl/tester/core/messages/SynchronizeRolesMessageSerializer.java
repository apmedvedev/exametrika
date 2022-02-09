/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.messages;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link SynchronizeRolesMessageSerializer} is a serializer for {@link SynchronizeRolesMessage}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SynchronizeRolesMessageSerializer extends AbstractSerializer {
    public static final UUID ID = UUID.fromString("0e2db7bf-0a07-41b2-86f0-96beef0991dd");

    public SynchronizeRolesMessageSerializer() {
        super(ID, SynchronizeRolesMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        SynchronizeRolesMessage part = (SynchronizeRolesMessage) object;

        serialization.writeInt(part.getRoles().size());
        for (String role : part.getRoles())
            serialization.writeString(role);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        int count = deserialization.readInt();
        Set<String> roles = new LinkedHashSet<String>(count);
        for (int i = 0; i < count; i++)
            roles.add(deserialization.readString());

        return new SynchronizeRolesMessage(roles);
    }
}
