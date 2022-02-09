/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server;

import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.DataDeserialization;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.agent.messages.MeasurementsMessage;
import com.exametrika.impl.agent.messages.RemoveNamesMessage;
import com.exametrika.impl.aggregator.common.model.DeserializeNameDictionary;
import com.exametrika.impl.aggregator.common.model.MeasurementSerializers;
import com.exametrika.spi.aggregator.IAggregationService;
import com.exametrika.spi.aggregator.common.model.INameDictionary;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;


/**
 * The {@link MeasurementReceiver} is a receiver of measurements.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class MeasurementReceiver {
    protected static final long MIN_RESET_PERIOD = 60000;
    private final IDatabase database;
    private final IMarker marker;
    private IAggregationService aggregationService;
    private DeserializeNameDictionary dictionary;
    private long lastResetTime;
    private boolean modified;

    public MeasurementReceiver(IDatabase database, IMarker marker) {
        Assert.notNull(database);

        this.database = database;
        this.marker = marker;
    }

    public void receive(final MeasurementsMessage message) {
        database.transaction(new Operation(IOperation.DISABLE_NODES_UNLOAD) {
            @Override
            public void run(ITransaction transaction) {
                receiveInTransaction(transaction, message);
            }

            @Override
            public void onRolledBack() {
                modified = modified || dictionary.isModified();
                long currentTime = Times.getCurrentTime();
                if (dictionary != null && modified && (lastResetTime == 0 || currentTime > lastResetTime + MIN_RESET_PERIOD)) {
                    lastResetTime = currentTime;
                    modified = false;
                    dictionary.reset();
                    resetSenderDictionary();
                }
            }
        });
    }

    public void receive(final RemoveNamesMessage message) {
        database.transaction(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                receiveInTransaction(transaction, message);
            }
        });
    }

    public void reset() {
        database.transaction(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                if (dictionary != null)
                    dictionary.reset();
            }
        });
    }

    protected abstract void resetSenderDictionary();

    protected abstract void onMeasurementsReceived(MeasurementSet measurements);

    private void receiveInTransaction(ITransaction transaction, MeasurementsMessage message) {
        if (!ensureAggregationService(transaction))
            return;

        IAggregationSchema aggregationSchema = aggregationService.getAggregationSchema();
        if (message.getSchemaVersion() != aggregationSchema.getVersion())
            return;

        ByteInputStream inputStream = new ByteInputStream(message.getMeasurements().getBuffer(),
                message.getMeasurements().getOffset(), message.getMeasurements().getLength());
        DataDeserialization deserialization = new DataDeserialization(inputStream);
        deserialization.setExtension(DeserializeNameDictionary.EXTENTION_ID, dictionary);

        MeasurementSet measurements = MeasurementSerializers.deserializeMeasurementSet(deserialization,
                aggregationSchema, dictionary);

        try {
            aggregationService.aggregate(measurements);
        } finally {
            onMeasurementsReceived(measurements);
        }

        dictionary.clearModified();
    }

    private void receiveInTransaction(ITransaction transaction, RemoveNamesMessage message) {
        if (dictionary != null)
            dictionary.removeNames(message.getRemovedNames());
    }

    private boolean ensureAggregationService(ITransaction transaction) {
        if (aggregationService != null)
            return true;

        aggregationService = transaction.findDomainService(IAggregationService.NAME);
        if (aggregationService == null)
            return false;

        INameDictionary nameDictionary = transaction.findExtension(IPeriodNameManager.NAME);
        dictionary = new DeserializeNameDictionary(nameDictionary, marker);
        return true;
    }
}
