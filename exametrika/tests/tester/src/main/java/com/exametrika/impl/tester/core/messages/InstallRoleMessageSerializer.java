/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.messages;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link InstallRoleMessageSerializer} is a serializer for {@link InstallRoleMessage}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstallRoleMessageSerializer extends AbstractSerializer {
    public static final UUID ID = UUID.fromString("63cc46d4-d9ca-42e0-8e84-74b39fa30f05");

    public InstallRoleMessageSerializer() {
        super(ID, InstallRoleMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        InstallRoleMessage part = (InstallRoleMessage) object;

        serialization.writeString(part.getRoleName());
        serialization.writeString(part.getMd5Hash());
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        String roleName = deserialization.readString();
        String md5Hash = deserialization.readString();

        return new InstallRoleMessage(roleName, md5Hash);
    }
}
