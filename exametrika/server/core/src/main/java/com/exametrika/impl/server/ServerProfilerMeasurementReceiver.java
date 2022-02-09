/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server;

import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.utils.Assert;


/**
 * The {@link ServerProfilerMeasurementReceiver} is a server profiler receiver of measurements.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ServerProfilerMeasurementReceiver extends MeasurementReceiver {
    private final ServerProfilerMeasurementSender sender;

    public ServerProfilerMeasurementReceiver(IDatabase database, IMarker marker, ServerProfilerMeasurementSender sender) {
        super(database, marker);

        Assert.notNull(sender);

        this.sender = sender;
    }

    @Override
    protected void resetSenderDictionary() {
        sender.resetDictionary();
    }

    @Override
    protected void onMeasurementsReceived(MeasurementSet measurements) {
    }
}
