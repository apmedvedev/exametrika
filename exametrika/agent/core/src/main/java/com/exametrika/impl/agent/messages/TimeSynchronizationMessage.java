/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.messages;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;

/**
 * The {@link TimeSynchronizationMessage} is a time synchronization message.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TimeSynchronizationMessage implements IMessagePart {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final long agentTime;
    private final long serverTime;

    public TimeSynchronizationMessage(long agentTime, long serverTime) {
        this.agentTime = agentTime;
        this.serverTime = serverTime;
    }

    public long getAgentTime() {
        return agentTime;
    }

    public long getServerTime() {
        return serverTime;
    }

    @Override
    public int getSize() {
        return 16;
    }

    @Override
    public String toString() {
        return messages.toString(agentTime, serverTime).toString();
    }

    private interface IMessages {
        @DefaultMessage("agent-time: {0}, server-time: {1}")
        ILocalizedMessage toString(long agentTime, long serverTime);
    }
}

