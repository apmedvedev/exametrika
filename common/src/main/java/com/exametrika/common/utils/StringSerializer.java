/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;


/**
 * The {@link StringSerializer} is serializer for {@link String}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StringSerializer extends AbstractSerializer {
    private static final UUID ID = UUID.fromString("b44b0221-3ea4-45f4-8158-d95d1848af25");

    public StringSerializer() {
        super(ID, String.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        String value = (String) object;
        serialization.writeString(value);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        return deserialization.readString();
    }
}
