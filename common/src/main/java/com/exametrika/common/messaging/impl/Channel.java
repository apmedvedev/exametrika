/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging.impl;

import java.util.Collections;
import java.util.List;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IChannelObserver;
import com.exametrika.common.messaging.IConnectionProvider;
import com.exametrika.common.messaging.IFeed;
import com.exametrika.common.messaging.ILiveNodeProvider;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessageFactory;
import com.exametrika.common.messaging.ISink;
import com.exametrika.common.messaging.impl.protocols.AbstractProtocol;
import com.exametrika.common.messaging.impl.protocols.composite.ProtocolStack;
import com.exametrika.common.messaging.impl.protocols.failuredetection.ChannelObserver;
import com.exametrika.common.messaging.impl.protocols.failuredetection.LiveNodeManager;
import com.exametrika.common.messaging.impl.transports.ITransport;
import com.exametrika.common.tasks.IFlowController;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SyncCompletionHandler;

/**
 * The {@link Channel} represents a message-oriented channel.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class Channel implements IChannel {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final long TIMEOUT = 10000;
    protected final String channelName;
    protected final ILogger logger = Loggers.get(Channel.class);
    protected final IMarker marker;
    protected final LiveNodeManager liveNodeManager;
    protected final ChannelObserver channelObserver;
    protected final ProtocolStack protocolStack;
    protected final ITransport transport;
    protected final IMessageFactory messageFactory;
    protected final IConnectionProvider connectionProvider;
    protected final ICompartment compartment;
    protected final AbstractProtocol entry;
    protected volatile boolean started;
    protected volatile boolean stopped;

    public Channel(String channelName, LiveNodeManager liveNodeManager, ChannelObserver channelObserver,
                   ProtocolStack protocolStack, ITransport transport,
                   IMessageFactory messageFactory, IConnectionProvider connectionProvider, ICompartment compartment) {
        Assert.notNull(channelName);
        Assert.notNull(liveNodeManager);
        Assert.notNull(channelObserver);
        Assert.notNull(protocolStack);
        Assert.notNull(transport);
        Assert.notNull(messageFactory);
        Assert.notNull(connectionProvider);
        Assert.notNull(compartment);

        this.channelName = channelName;
        this.marker = Loggers.getMarker(channelName);
        this.liveNodeManager = liveNodeManager;
        this.channelObserver = channelObserver;
        this.protocolStack = protocolStack;
        this.transport = transport;
        this.messageFactory = messageFactory;
        this.connectionProvider = connectionProvider;
        this.compartment = compartment;
        this.entry = protocolStack.getFirst();
    }

    public ProtocolStack getProtocolStack() {
        return protocolStack;
    }

    @Override
    public ICompartment getCompartment() {
        return compartment;
    }

    @Override
    public IMessageFactory getMessageFactory() {
        return messageFactory;
    }

    @Override
    public void send(final IMessage message) {
        if (started && !stopped)
            compartment.offer(new MessageSendTask(entry, Collections.singletonList(message)));
    }

    @Override
    public void send(final List<IMessage> messages) {
        if (started && !stopped)
            compartment.offer(new MessageSendTask(entry, messages));
    }

    @Override
    public ISink register(IAddress destination, IFeed feed) {
        if (started && !stopped)
            return entry.register(destination, feed);
        else
            return null;
    }

    @Override
    public void unregister(ISink sink) {
        entry.unregister(sink);
    }

    @Override
    public ILiveNodeProvider getLiveNodeProvider() {
        return liveNodeManager;
    }

    @Override
    public IChannelObserver getChannelObserver() {
        return channelObserver;
    }

    @Override
    public void connect(String connection) {
        connectionProvider.connect(connection);
    }

    @Override
    public void connect(IAddress connection) {
        connectionProvider.connect(connection);
    }

    @Override
    public void disconnect(String connection) {
        connectionProvider.disconnect(connection);
    }

    @Override
    public void disconnect(IAddress connection) {
        connectionProvider.disconnect(connection);
    }

    @Override
    public String canonicalize(String connection) {
        return connectionProvider.canonicalize(connection);
    }

    @Override
    public void start() {
        synchronized (this) {
            Assert.checkState(!started);

            started = true;
        }

        compartment.start();
        channelObserver.start();

        final SyncCompletionHandler startHandler = new SyncCompletionHandler();
        compartment.offer(new Runnable() {
            @Override
            public void run() {
                try {
                    protocolStack.start();
                    transport.start();

                    liveNodeManager.start();

                    doStart();

                    startHandler.onSucceeded(null);
                } catch (Throwable e) {
                    startHandler.onFailed(e);
                }
            }
        });
        startHandler.await(TIMEOUT);

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.channelStarted());
    }

    @Override
    public void stop() {
        synchronized (this) {
            if (!started || stopped)
                return;

            stopped = true;
        }

        final SyncCompletionHandler stopHandler = new SyncCompletionHandler();
        compartment.offer(new Runnable() {
            @Override
            public void run() {
                try {
                    protocolStack.stop();
                    transport.stop();
                    liveNodeManager.stop();

                    doStop();

                    stopHandler.onSucceeded(null);
                } catch (Throwable e) {
                    stopHandler.onFailed(e);
                }
            }
        });
        stopHandler.await(TIMEOUT);

        channelObserver.stop();
        compartment.stop();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.channelStopped());
    }

    @Override
    public IFlowController<IAddress> getFlowController() {
        return transport;
    }

    @Override
    public String toString() {
        return channelName;
    }

    protected void doStart() {
    }

    protected void doStop() {
    }

    private interface IMessages {
        @DefaultMessage("Channel has been started.")
        ILocalizedMessage channelStarted();

        @DefaultMessage("Channel has been stopped.")
        ILocalizedMessage channelStopped();
    }
}
