/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server;

import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.agent.messages.ResetDictionaryMessage;


/**
 * The {@link ChannelMeasurementReceiver} is a server agent channel receiver of measurements.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ChannelMeasurementReceiver extends MeasurementReceiver {
    private final ServerAgentChannel agent;
    private final IChannel channel;
    private final MeasurementRequestor measurementRequestor;

    public ChannelMeasurementReceiver(IDatabase database, IMarker marker, ServerAgentChannel agent, IChannel channel,
                                      MeasurementRequestor measurementRequestor) {
        super(database, marker);

        Assert.notNull(agent);
        Assert.notNull(channel);
        Assert.notNull(measurementRequestor);

        this.agent = agent;
        this.channel = channel;
        this.measurementRequestor = measurementRequestor;
    }

    @Override
    protected void resetSenderDictionary() {
        IMessage message = channel.getMessageFactory().create(agent.getNode(), new ResetDictionaryMessage());
        channel.send(message);
    }

    @Override
    protected void onMeasurementsReceived(MeasurementSet measurements) {
        measurementRequestor.onMeasurementsReceived(agent.getNode(), measurements);
    }
}
