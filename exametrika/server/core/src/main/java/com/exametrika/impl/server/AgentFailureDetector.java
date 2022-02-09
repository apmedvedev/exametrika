/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server;

import java.util.ArrayList;

import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.spi.component.IAgentFailureDetector;
import com.exametrika.spi.component.IAgentFailureListener;


/**
 * The {@link AgentFailureDetector} is an agent failure detector.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AgentFailureDetector implements IAgentFailureDetector {
    private static final ILogger logger = Loggers.get(AgentFailureDetector.class);
    private volatile ArrayList<IAgentFailureListener> listeners = new ArrayList<IAgentFailureListener>();
    private volatile IChannel channel;
    private volatile boolean disableAgentFailures;

    public synchronized void setChannel(IChannel channel) {
        Assert.notNull(channel);

        this.channel = channel;
    }

    public void disableAgentFailures(boolean value) {
        disableAgentFailures = value;
    }

    @Override
    public boolean isActive(String agentId) {
        IChannel channel = this.channel;
        if (channel != null)
            return channel.getLiveNodeProvider().findByName(agentId) != null;
        else
            return false;
    }

    @Override
    public synchronized void addFailureListener(IAgentFailureListener listener) {
        Assert.notNull(listener);

        ArrayList<IAgentFailureListener> listeners = (ArrayList<IAgentFailureListener>) this.listeners.clone();
        listeners.add(listener);

        this.listeners = listeners;
    }

    @Override
    public synchronized void removeFailureListener(IAgentFailureListener listener) {
        Assert.notNull(listener);

        ArrayList<IAgentFailureListener> listeners = (ArrayList<IAgentFailureListener>) this.listeners.clone();
        listeners.remove(listener);

        this.listeners = listeners;
    }

    public void fireOnAgentActivated(String agentId) {
        for (IAgentFailureListener listener : listeners) {
            try {
                listener.onAgentActivated(agentId);
            } catch (ThreadInterruptedException e) {
                throw e;
            } catch (Exception e) {
                Exceptions.checkInterrupted(e);

                // Isolate exception from other listeners
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }
        }
    }

    public void fireOnAgentFailed(String agentId) {
        boolean disableAgentFailures = this.disableAgentFailures;
        if (disableAgentFailures)
            return;

        for (IAgentFailureListener listener : listeners) {
            try {
                listener.onAgentFailed(agentId);
            } catch (ThreadInterruptedException e) {
                throw e;
            } catch (Exception e) {
                Exceptions.checkInterrupted(e);

                // Isolate exception from other listeners
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }
        }
    }
}
