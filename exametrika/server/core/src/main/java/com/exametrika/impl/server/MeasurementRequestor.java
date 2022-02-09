/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server;

import java.util.Iterator;
import java.util.Set;

import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.agent.messages.RequestMeasurementsMessage;
import com.exametrika.spi.aggregator.IMeasurementRequestor;


/**
 * The {@link MeasurementRequestor} is a measurements requestor.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MeasurementRequestor implements IMeasurementRequestor {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(MeasurementRequestor.class);
    private static final long WAIT_TIMEOUT = 60000;
    private volatile ServerChannel serverChannel;
    private volatile IChannel channel;
    private ICompletionHandler completionHandler;
    private Set<IAddress> agents;
    private long startTime;

    public void setServerChannel(ServerChannel serverChannel) {
        Assert.notNull(serverChannel);

        this.serverChannel = serverChannel;
    }

    public synchronized void setChannel(IChannel channel) {
        Assert.notNull(channel);

        this.channel = channel;
    }

    @Override
    public void requestMeasurements(ICompletionHandler completionHandler) {
        Set<IAddress> agents = serverChannel.getAgents();
        if (agents.isEmpty()) {
            completionHandler.onSucceeded(null);
            return;
        }

        synchronized (this) {
            Assert.notNull(completionHandler);
            Assert.isNull(this.completionHandler);

            this.completionHandler = completionHandler;
            this.agents = agents;
            this.startTime = Times.getCurrentTime();

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.measurementsRequested(agents));

            for (Iterator<IAddress> it = agents.iterator(); it.hasNext(); ) {
                IAddress agent = it.next();
                if (!channel.getLiveNodeProvider().isLive(agent))
                    it.remove();
                else {
                    IMessage message = channel.getMessageFactory().create(agent, new RequestMeasurementsMessage());
                    channel.send(message);
                }
            }
        }
    }

    public void onMeasurementsReceived(IAddress agent, MeasurementSet measurements) {
        synchronized (this) {
            if (agents == null)
                return;
        }

        if (measurements.isResponse())
            onCompleted(agent);
    }

    public void onAgentFailed(IAddress agent) {
        onCompleted(agent);
    }

    public synchronized void onTimer(long currentTime) {
        if (agents == null)
            return;

        if (currentTime > startTime + WAIT_TIMEOUT) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, messages.waitFailed(agents));

            completionHandler.onFailed(null);

            completionHandler = null;
            agents = null;
            this.startTime = 0;
        }
    }

    private synchronized void onCompleted(IAddress agent) {
        if (agents == null)
            return;

        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, messages.agentWaitCompleted(agent));

        agents.remove(agent);
        if (!agents.isEmpty())
            return;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.waitCompleted());

        completionHandler.onSucceeded(null);

        completionHandler = null;
        agents = null;
        this.startTime = 0;
    }

    private interface IMessages {
        @DefaultMessage("Measurements request has been failed. The following agents did not responded: {0}.")
        ILocalizedMessage waitFailed(Set<IAddress> agents);

        @DefaultMessage("Measurements are requested for agents: {0}.")
        ILocalizedMessage measurementsRequested(Set<IAddress> agents);

        @DefaultMessage("Measurements are received from agent: {0}.")
        ILocalizedMessage agentWaitCompleted(IAddress agent);

        @DefaultMessage("Measurements request has been completed.")
        ILocalizedMessage waitCompleted();
    }
}
