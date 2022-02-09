/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.messages;

import java.util.UUID;

import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Assert;

/**
 * The {@link ResponseMessageSerializer} is a serializer for {@link ResponseMessage}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ResponseMessageSerializer extends AbstractSerializer {
    public static final UUID ID = UUID.fromString("4643893b-1118-410d-af64-09bf77561fde");

    public ResponseMessageSerializer() {
        super(ID, ResponseMessage.class);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        ResponseMessage part = (ResponseMessage) object;
        Assert.notNull(part);

        serialization.writeString(part.getTestCaseName());
        serialization.writeString(part.getNodeName());
        serialization.writeBoolean(part.isResultsOnly());
        serialization.writeObject(part.getException());
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        String testCaseName = deserialization.readString();
        String nodeName = deserialization.readString();
        boolean resultsOnly = deserialization.readBoolean();
        Throwable exception = deserialization.readObject();
        return new ResponseMessage(testCaseName, nodeName, resultsOnly, exception);
    }
}
