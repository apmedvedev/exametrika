/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.messages;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link PlatformUpdateMessageSerializer} is a serializer for {@link PlatformUpdateMessage}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PlatformUpdateMessageSerializer extends AbstractSerializer {
    public static final UUID ID = UUID.fromString("8f72d39a-f0af-4ccf-b02e-97f568a6055e");

    public PlatformUpdateMessageSerializer() {
        super(ID, PlatformUpdateMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        PlatformUpdateMessage part = (PlatformUpdateMessage) object;

        serialization.writeString(part.getConfiguration());
        serialization.writeString(part.getConfigHash());
        serialization.writeString(part.getModulesHash());
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        String configuration = deserialization.readString();
        String configHash = deserialization.readString();
        String modulesHash = deserialization.readString();

        return new PlatformUpdateMessage(configuration, configHash, modulesHash);
    }
}
