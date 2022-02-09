/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent;

import java.io.File;
import java.util.List;

import com.exametrika.api.agent.config.AgentConfiguration;
import com.exametrika.api.profiler.IProfilingService;
import com.exametrika.common.io.ISerializationRegistrar;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IChannelListener;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.services.IServiceRegistry;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ILifecycle;
import com.exametrika.impl.agent.messages.ActionMessage;
import com.exametrika.impl.agent.messages.PlatformUpdateMessage;
import com.exametrika.impl.agent.messages.RequestMeasurementsMessage;
import com.exametrika.impl.agent.messages.ResetDictionaryMessage;
import com.exametrika.impl.agent.messages.TimeSynchronizationMessage;
import com.exametrika.impl.boot.utils.IHotDeployer;


/**
 * The {@link AgentChannel} represents an agent channel.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AgentChannel implements IReceiver, IChannelListener, ISerializationRegistrar, ILifecycle {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(AgentChannel.class);
    private final AgentConfiguration configuration;
    private final MeasurementSender measurementSender;
    private final AgentPlatformUpdater platformUpdater;
    private final ActionExecutionManager actionExecutionManager;
    private final TimeSynchronizer timeSynchronizer;
    private final IServiceRegistry serviceRegistry;
    private final long minReconnectPeriod;
    private final IMarker marker;
    private final IProfilingService profilingService;
    private IChannel channel;
    private volatile State state;
    private long currentTime;
    private IAddress serverNode;
    private long lastConnectTime;

    public AgentChannel(AgentConfiguration configuration, IHotDeployer hotDeployer, IServiceRegistry serviceRegistry,
                        IProfilingService profilingService) {
        Assert.notNull(configuration);
        Assert.notNull(serviceRegistry);

        this.configuration = configuration;
        this.measurementSender = new MeasurementSender(this);
        this.platformUpdater = new AgentPlatformUpdater(configuration, hotDeployer, this);
        this.actionExecutionManager = new ActionExecutionManager(this);
        this.timeSynchronizer = new TimeSynchronizer(this);
        this.serviceRegistry = serviceRegistry;
        this.profilingService = profilingService;

        if (configuration.getChannel().getTransport() != null)
            minReconnectPeriod = configuration.getChannel().getTransport().getTransportMinReconnectPeriod();
        else
            minReconnectPeriod = 60000;

        marker = Loggers.getMarker(configuration.getName());
    }

    public String getName() {
        return configuration.getName();
    }

    public boolean isConnected() {
        return state == State.SYNCHRONIZED;
    }

    public MeasurementSender getMeasurementSender() {
        return measurementSender;
    }

    public IServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public void setChannel(IChannel channel) {
        Assert.notNull(channel);
        Assert.isNull(this.channel);

        this.channel = channel;
    }

    public synchronized void onTimer(long currentTime) {
        this.currentTime = currentTime;

        if (state == State.STARTED && (lastConnectTime == 0 || (currentTime - lastConnectTime) > minReconnectPeriod))
            connect();

        if (isConnected())
            measurementSender.onTimer(currentTime);
    }

    @Override
    public synchronized void start() {
        Assert.checkState(channel != null);
        Assert.isNull(state);

        this.state = State.STARTED;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.started());

        actionExecutionManager.start();
        channel.start();

        connect();
    }

    @Override
    public void stop() {
        synchronized (this) {
            if (state == null)
                return;

            disconnect();
            state = null;
        }

        channel.stop();
        actionExecutionManager.stop();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.stopped());
    }

    @Override
    public synchronized void onNodeConnected(IAddress node) {
        state = State.CONNECTED;
        serverNode = node;
        measurementSender.onConnected();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.connected(node));

        timeSynchronizer.requestTimeSynchronization();
        platformUpdater.requestAgentStart();
    }

    @Override
    public synchronized void onNodeFailed(IAddress node) {
        disconnect();
    }

    @Override
    public synchronized void onNodeDisconnected(IAddress node) {
        disconnect();
    }

    @Override
    public void receive(final IMessage message) {
        if (message.getPart() instanceof PlatformUpdateMessage) {
            channel.getCompartment().execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        platformUpdater.updatePlatform((PlatformUpdateMessage) message.getPart(), message.getFiles());
                        state = State.SYNCHRONIZED;
                    }

                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, marker, messages.platformUpdated());
                }
            });
        } else if (message.getPart() instanceof ActionMessage)
            actionExecutionManager.handle((ActionMessage) message.getPart());
        else if (message.getPart() instanceof TimeSynchronizationMessage)
            timeSynchronizer.receive((TimeSynchronizationMessage) message.getPart());
        else if (message.getPart() instanceof ResetDictionaryMessage) {
            synchronized (this) {
                measurementSender.resetDictionary();
            }
        } else if (message.getPart() instanceof RequestMeasurementsMessage) {
            if (profilingService != null)
                profilingService.requestMeasurements();
        }
    }

    @Override
    public void register(ISerializationRegistry registry) {
        measurementSender.register(registry);
        platformUpdater.register(registry);
        actionExecutionManager.register(registry);
        timeSynchronizer.register(registry);
    }

    @Override
    public void unregister(ISerializationRegistry registry) {
        measurementSender.unregister(registry);
        platformUpdater.unregister(registry);
        actionExecutionManager.unregister(registry);
        timeSynchronizer.unregister(registry);
    }

    public void send(IMessagePart part) {
        IMessage message = channel.getMessageFactory().create(serverNode, part);
        channel.send(message);
    }

    public void send(IMessagePart part, List<File> files) {
        IMessage message = channel.getMessageFactory().create(serverNode, part, 0, files);
        channel.send(message);
    }

    private void connect() {
        String serverAddress = "tcp://" + configuration.getChannel().getServerHost() + ":" + configuration.getChannel().getServerPort();
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.connecting(serverAddress));

        channel.connect(serverAddress);
        lastConnectTime = currentTime;
    }

    private void disconnect() {
        state = State.STARTED;
        lastConnectTime = currentTime;
        measurementSender.onDisconnected();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.disconnected());
    }

    private enum State {
        STARTED,
        CONNECTED,
        SYNCHRONIZED
    }

    private interface IMessages {
        @DefaultMessage("Agent channel is started.")
        ILocalizedMessage started();

        @DefaultMessage("Agent channel is connecting to server ''{0}''.")
        ILocalizedMessage connecting(String server);

        @DefaultMessage("Agent channel is connected to server ''{0}''.")
        ILocalizedMessage connected(IAddress server);

        @DefaultMessage("Platform is updated.")
        ILocalizedMessage platformUpdated();

        @DefaultMessage("Agent channel is disconnected.")
        ILocalizedMessage disconnected();

        @DefaultMessage("Agent channel is stopped.")
        ILocalizedMessage stopped();
    }
}
