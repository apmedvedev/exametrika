/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.messages;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Assert;

/**
 * The {@link ResetDictionaryMessageSerializer} is a serializer for {@link ResetDictionaryMessage}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ResetDictionaryMessageSerializer extends AbstractSerializer {
    public static final UUID ID = UUID.fromString("d50ec955-5065-4ce4-81a0-77717255e4bb");

    public ResetDictionaryMessageSerializer() {
        super(ID, ResetDictionaryMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        ResetDictionaryMessage part = (ResetDictionaryMessage) object;
        Assert.notNull(part);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        return new ResetDictionaryMessage();
    }
}
