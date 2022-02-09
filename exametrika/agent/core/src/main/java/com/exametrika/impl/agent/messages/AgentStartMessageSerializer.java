/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.messages;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;

/**
 * The {@link AgentStartMessageSerializer} is a serializer for {@link AgentStartMessage}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AgentStartMessageSerializer extends AbstractSerializer {
    public static final UUID ID = UUID.fromString("b4900a5e-4bb0-4290-a246-627abb220824");

    public AgentStartMessageSerializer() {
        super(ID, AgentStartMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        AgentStartMessage part = (AgentStartMessage) object;

        serialization.writeString(part.getComponent());
        serialization.writeString(part.getConfigHash());
        serialization.writeString(part.getModulesHash());
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        String component = deserialization.readString();
        String configHash = deserialization.readString();
        String modulesHash = deserialization.readString();

        return new AgentStartMessage(component, configHash, modulesHash);
    }
}
