/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.agent.messages.TimeSynchronizationMessage;
import com.exametrika.impl.agent.messages.TimeSynchronizationMessageSerializer;


/**
 * The {@link TimeSynchronizer} represents a time synchronizer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TimeSynchronizer {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(AgentService.class);
    private final AgentChannel channel;

    public TimeSynchronizer(AgentChannel channel) {
        Assert.notNull(channel);

        this.channel = channel;
    }

    public void register(ISerializationRegistry registry) {
        registry.register(new TimeSynchronizationMessageSerializer());
    }

    public void unregister(ISerializationRegistry registry) {
        registry.unregister(TimeSynchronizationMessageSerializer.ID);
    }

    public void requestTimeSynchronization() {
        channel.send(new TimeSynchronizationMessage(Times.getSystemCurrentTime(), 0));
    }

    public void receive(TimeSynchronizationMessage message) {
        long currentTime = Times.getSystemCurrentTime();
        long delta = message.getServerTime() - message.getAgentTime() - (currentTime - message.getAgentTime()) / 2;
        if (Math.abs(delta) > 10000) {
            if (Math.abs(delta) < 60000) {
                Times.setDelta(delta);

                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.timeSynchronized(delta));
            } else if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, messages.timeDeltaTooLarge(delta / 1000));
        }
    }

    private interface IMessages {
        @DefaultMessage("Agent time is synchronized with server, delta: {0}.")
        ILocalizedMessage timeSynchronized(long delta);

        @DefaultMessage("The difference between the time of agent and server is too large: {0}s.")
        ILocalizedMessage timeDeltaTooLarge(long delta);
    }
}
