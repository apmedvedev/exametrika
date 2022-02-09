/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.messages;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;

/**
 * The {@link MeasurementsMessage} is a measurement schema synchronization message.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MeasurementsMessage implements IMessagePart {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final int schemaVersion;
    private final ByteArray measurements;

    public MeasurementsMessage(int schemaVersion, ByteArray measurements) {
        Assert.notNull(measurements);

        this.schemaVersion = schemaVersion;
        this.measurements = measurements;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public ByteArray getMeasurements() {
        return measurements;
    }

    @Override
    public int getSize() {
        return measurements.getLength() + 5;
    }

    @Override
    public String toString() {
        return messages.toString(schemaVersion, measurements.getLength()).toString();
    }

    private interface IMessages {
        @DefaultMessage("schema version: {0}, measurements length: {1}")
        ILocalizedMessage toString(int schemaVersion, int length);
    }
}

