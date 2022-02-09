/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent;

import java.util.List;

import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.DataSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.impl.agent.messages.MeasurementsMessage;
import com.exametrika.impl.agent.messages.MeasurementsMessageSerializer;
import com.exametrika.impl.agent.messages.RemoveNamesMessage;
import com.exametrika.impl.agent.messages.RemoveNamesMessageSerializer;
import com.exametrika.impl.agent.messages.RequestMeasurementsMessageSerializer;
import com.exametrika.impl.agent.messages.ResetDictionaryMessageSerializer;
import com.exametrika.impl.aggregator.common.model.MeasurementSerializers;
import com.exametrika.impl.aggregator.common.model.SerializeNameDictionary;
import com.exametrika.impl.aggregator.common.model.SerializeNameDictionary.SerializeNameId;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;


/**
 * The {@link MeasurementSender} represents a measurement sender.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MeasurementSender {
    private static final long CLEANUP_PERIOD = 60000;
    private final AgentChannel channel;
    private final SerializeNameDictionary dictionary = new SerializeNameDictionary();
    private volatile IAggregationSchema schema;
    private long lastCleanupTime;
    private boolean connected;

    public MeasurementSender(AgentChannel channel) {
        Assert.notNull(channel);

        this.channel = channel;
    }

    public void register(ISerializationRegistry registry) {
        registry.register(new MeasurementsMessageSerializer());
        registry.register(new RemoveNamesMessageSerializer());
        registry.register(new ResetDictionaryMessageSerializer());
        registry.register(new RequestMeasurementsMessageSerializer());
    }

    public void unregister(ISerializationRegistry registry) {
        registry.unregister(MeasurementsMessageSerializer.ID);
        registry.unregister(RemoveNamesMessageSerializer.ID);
        registry.unregister(ResetDictionaryMessageSerializer.ID);
        registry.unregister(RequestMeasurementsMessageSerializer.ID);
    }

    public void onConnected() {
        dictionary.reset();
        connected = true;
    }

    public void onDisconnected() {
        dictionary.reset();
        connected = false;
    }

    public void resetDictionary() {
        dictionary.reset();
    }

    public void onTimer(long currentTime) {
        if (lastCleanupTime == 0 || currentTime - lastCleanupTime > CLEANUP_PERIOD) {
            List<SerializeNameId> removedNames = null;
            synchronized (channel) {
                if (connected)
                    removedNames = dictionary.takeRemovedNames();
            }
            if (!removedNames.isEmpty())
                channel.send(new RemoveNamesMessage(removedNames));

            lastCleanupTime = currentTime;
        }
    }

    public boolean canHandle() {
        return channel.isConnected();
    }

    public void handle(MeasurementSet measurements) {
        IAggregationSchema schema = this.schema;
        if (schema == null || schema.getVersion() != measurements.getSchemaVersion())
            return;

        ByteOutputStream outputStream = new ByteOutputStream();
        DataSerialization serialization = new DataSerialization(outputStream);

        serialization.setExtension(SerializeNameDictionary.EXTENTION_ID, dictionary);

        synchronized (channel) {
            if (!connected)
                return;

            MeasurementSerializers.serializeMeasurementSet(serialization, measurements, schema, dictionary);
        }

        ByteArray buffer = new ByteArray(outputStream.getBuffer(), 0, outputStream.getLength());
        channel.send(new MeasurementsMessage(measurements.getSchemaVersion(), buffer));
    }

    public void setSchema(IAggregationSchema schema) {
        this.schema = schema;
    }
}
