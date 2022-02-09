/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server;

import java.util.List;

import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.DataSerialization;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.impl.agent.messages.MeasurementsMessage;
import com.exametrika.impl.agent.messages.RemoveNamesMessage;
import com.exametrika.impl.aggregator.common.model.MeasurementSerializers;
import com.exametrika.impl.aggregator.common.model.SerializeNameDictionary;
import com.exametrika.impl.aggregator.common.model.SerializeNameDictionary.SerializeNameId;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;


/**
 * The {@link ServerProfilerMeasurementSender} represents a measurement sender of server profiler.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ServerProfilerMeasurementSender {
    private static final long CLEANUP_PERIOD = 60000;
    private final ServerProfilerMeasurementReceiver receiver;
    private final SerializeNameDictionary dictionary = new SerializeNameDictionary();
    private volatile IAggregationSchema schema;
    private long lastCleanupTime;

    public ServerProfilerMeasurementSender(IDatabase database, IAggregationSchema schema) {
        this.receiver = new ServerProfilerMeasurementReceiver(database, Loggers.getMarker("server"), this);
        this.schema = schema;
    }

    public void onTimer(long currentTime) {
        if (lastCleanupTime == 0 || currentTime - lastCleanupTime > CLEANUP_PERIOD) {
            List<SerializeNameId> removedNames = null;
            synchronized (receiver) {
                removedNames = dictionary.takeRemovedNames();
            }

            if (!removedNames.isEmpty())
                receiver.receive(new RemoveNamesMessage(removedNames));

            lastCleanupTime = currentTime;
        }
    }

    public void handle(MeasurementSet measurements) {
        IAggregationSchema schema = this.schema;
        if (schema == null || schema.getVersion() != measurements.getSchemaVersion())
            return;

        ByteOutputStream outputStream = new ByteOutputStream();
        DataSerialization serialization = new DataSerialization(outputStream);

        serialization.setExtension(SerializeNameDictionary.EXTENTION_ID, dictionary);

        synchronized (receiver) {
            MeasurementSerializers.serializeMeasurementSet(serialization, measurements, schema, dictionary);
        }

        ByteArray buffer = new ByteArray(outputStream.getBuffer(), 0, outputStream.getLength());
        receiver.receive(new MeasurementsMessage(measurements.getSchemaVersion(), buffer));
    }

    public void setSchema(IAggregationSchema schema) {
        this.schema = schema;
    }

    public void resetDictionary() {
        synchronized (receiver) {
            dictionary.reset();
        }
    }
}
