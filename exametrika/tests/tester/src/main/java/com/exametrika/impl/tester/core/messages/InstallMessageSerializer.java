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
 * The {@link InstallMessageSerializer} is a serializer for {@link InstallMessage}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstallMessageSerializer extends AbstractSerializer {
    public static final UUID ID = UUID.fromString("9608c042-7175-44b6-b8a0-6031552a35b0");

    public InstallMessageSerializer() {
        super(ID, InstallMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        InstallMessage part = (InstallMessage) object;

        serialization.writeString(part.getTestCaseName());
        serialization.writeString(part.getRoleName());
        serialization.writeString(part.getNodeName());
        serialization.writeString(part.getExecutorName());
        serialization.writeInt(part.getExecutorParameters().size());
        for (Map.Entry<String, Object> entry : part.getExecutorParameters().entrySet()) {
            serialization.writeString(entry.getKey());
            serialization.writeObject(entry.getValue());
        }
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        String testCaseName = deserialization.readString();
        String roleName = deserialization.readString();
        String nodeName = deserialization.readString();
        String executorName = deserialization.readString();
        int count = deserialization.readInt();
        Map<String, Object> executorParameters = new LinkedHashMap<String, Object>(count);
        for (int i = 0; i < count; i++)
            executorParameters.put(deserialization.readString(), deserialization.readObject());

        return new InstallMessage(testCaseName, roleName, nodeName, executorName, executorParameters);
    }
}
