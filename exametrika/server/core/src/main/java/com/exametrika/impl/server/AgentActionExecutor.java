/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.agent.messages.ActionMessage;
import com.exametrika.spi.component.IAgentActionExecutor;


/**
 * The {@link AgentActionExecutor} is an agent action executor.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AgentActionExecutor implements IAgentActionExecutor {
    private static final IMessages messages = Messages.get(IMessages.class);
    private Map<Pair<String, Long>, ICompletionHandler> handlers = new HashMap<Pair<String, Long>, ICompletionHandler>();
    private long nextActionId = 1;
    private IChannel channel;

    public synchronized void setChannel(IChannel channel) {
        Assert.notNull(channel);

        this.channel = channel;
    }

    @Override
    public <T> void execute(String agentId, Object action, ICompletionHandler<T> completionHandler) {
        Assert.notNull(completionHandler);
        Assert.notNull(action);

        IMessage message = null;
        ChannelException exception = null;
        synchronized (this) {
            if (channel == null) {
                exception = new ChannelException(messages.channelNotSet());
                exception.setStackTrace(new StackTraceElement[0]);
            } else {
                IAddress agentAddress = channel.getLiveNodeProvider().findByName(agentId);
                if (agentAddress == null) {
                    exception = new ChannelException(messages.agentNotConnected(agentId));
                    exception.setStackTrace(new StackTraceElement[0]);
                } else {
                    long actionId = nextActionId++;
                    handlers.put(new Pair<String, Long>(agentId, actionId), completionHandler);
                    message = channel.getMessageFactory().create(agentAddress, new ActionMessage(actionId, action));
                }
            }
        }

        if (message != null)
            channel.send(message);
        else {
            Assert.notNull(exception);
            completionHandler.onFailed(exception);
        }
    }

    public void receive(String agentId, ActionMessage message) {
        ICompletionHandler handler;
        synchronized (this) {
            handler = handlers.remove(new Pair<String, Long>(agentId, message.getActionId()));
            if (handler == null)
                return;
        }

        if (message.getAction() instanceof Throwable)
            handler.onFailed((Throwable) message.getAction());
        else
            handler.onSucceeded(message.getAction());
    }

    public void onAgentFailed(String agentId) {
        List<ICompletionHandler> handlers = new ArrayList<ICompletionHandler>();
        synchronized (this) {
            for (Iterator<Map.Entry<Pair<String, Long>, ICompletionHandler>> it = this.handlers.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Pair<String, Long>, ICompletionHandler> entry = it.next();
                if (entry.getKey().getKey().equals(agentId)) {
                    handlers.add(entry.getValue());
                    it.remove();
                }
            }
        }

        ChannelException exception = new ChannelException(messages.agentFailed(agentId));
        exception.setStackTrace(new StackTraceElement[0]);

        for (ICompletionHandler handler : handlers)
            handler.onFailed(exception);
    }

    private interface IMessages {
        @DefaultMessage("Agent ''{0}'' has been failed.")
        ILocalizedMessage agentFailed(String agentId);

        @DefaultMessage("Agent ''{0}'' is not connected to server.")
        ILocalizedMessage agentNotConnected(String agentId);

        @DefaultMessage("Server channel is not set.")
        ILocalizedMessage channelNotSet();
    }
}
