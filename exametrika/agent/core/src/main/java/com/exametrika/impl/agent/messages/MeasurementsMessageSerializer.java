/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.messages;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.ByteArray;

/**
 * The {@link MeasurementsMessageSerializer} is a serializer for {@link MeasurementsMessage}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MeasurementsMessageSerializer extends AbstractSerializer {
    public static final UUID ID = UUID.fromString("584fd464-31d8-45a2-8d83-f9240b51b51e");

    public MeasurementsMessageSerializer() {
        super(ID, MeasurementsMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        MeasurementsMessage part = (MeasurementsMessage) object;

        serialization.writeInt(part.getSchemaVersion());
        serialization.writeByteArray(part.getMeasurements());
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        int schemaVersion = deserialization.readInt();
        ByteArray measurements = deserialization.readByteArray();

        return new MeasurementsMessage(schemaVersion, measurements);
    }
}
