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
 * The {@link RequestMeasurementsMessageSerializer} is a serializer for {@link RequestMeasurementsMessage}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class RequestMeasurementsMessageSerializer extends AbstractSerializer {
    public static final UUID ID = UUID.fromString("7504c9c6-cd76-4103-bf12-8523c58a32e7");

    public RequestMeasurementsMessageSerializer() {
        super(ID, RequestMeasurementsMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        RequestMeasurementsMessage part = (RequestMeasurementsMessage) object;
        Assert.notNull(part);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        return new RequestMeasurementsMessage();
    }
}
