/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.coordinator;

import java.io.File;
import java.util.List;

import com.exametrika.api.tester.config.TestAgentConnectionConfiguration;
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
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ILifecycle;
import com.exametrika.impl.tester.core.messages.ActionMessageSerializer;
import com.exametrika.impl.tester.core.messages.ActionResponseMessageSerializer;
import com.exametrika.impl.tester.core.messages.ControlMessageSerializer;
import com.exametrika.impl.tester.core.messages.InstallMessageSerializer;
import com.exametrika.impl.tester.core.messages.InstallRoleMessageSerializer;
import com.exametrika.impl.tester.core.messages.ResponseMessageSerializer;
import com.exametrika.impl.tester.core.messages.SynchronizeRolesMessageSerializer;
import com.exametrika.impl.tester.core.messages.SynchronizeRolesResponseMessageSerializer;


/**
 * The {@link TestCoordinatorChannel} represents a coordinator channel.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestCoordinatorChannel implements IReceiver, IChannelListener, ISerializationRegistrar, ILifecycle {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(TestCoordinatorChannel.class);
    private final TestAgentConnectionConfiguration configuration;
    private final TestCoordinatorService service;
    private final long minReconnectPeriod;
    private final IMarker marker;
    private IChannel channel;
    private volatile State state;
    private long currentTime;
    private IAddress address;
    private long lastConnectTime;

    public TestCoordinatorChannel(TestAgentConnectionConfiguration configuration, TestCoordinatorService service) {
        Assert.notNull(configuration);
        Assert.notNull(service);

        this.configuration = configuration;
        this.service = service;
        minReconnectPeriod = 60000;

        marker = Loggers.getMarker(configuration.toString());
    }

    public IAddress getAddress() {
        return address;
    }

    public TestAgentConnectionConfiguration getConfiguration() {
        return configuration;
    }

    public void setChannel(IChannel channel) {
        Assert.notNull(channel);
        Assert.isNull(this.channel);

        this.channel = channel;
    }

    public void onTimer(long currentTime) {
        this.currentTime = currentTime;

        if (state == State.STARTED && (lastConnectTime == 0 || (currentTime - lastConnectTime) > minReconnectPeriod))
            connect();
    }

    @Override
    public void start() {
        Assert.checkState(channel != null);
        Assert.isNull(state);

        this.state = State.STARTED;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.started());

        channel.start();

        connect();
    }

    @Override
    public void stop() {
        if (state == null)
            return;

        disconnect();
        state = null;

        channel.stop();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.stopped());
    }

    @Override
    public void onNodeConnected(IAddress node) {
        synchronized (service) {
            state = State.CONNECTED;
            address = node;
            service.addChannel(address, this);

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.connected(node));
        }
    }

    @Override
    public void onNodeFailed(IAddress node) {
        synchronized (service) {
            disconnect();
        }
    }

    @Override
    public void onNodeDisconnected(IAddress node) {
        synchronized (service) {
            disconnect();
        }
    }

    @Override
    public void receive(final IMessage message) {
        channel.getCompartment().execute(new Runnable() {
            @Override
            public void run() {
                synchronized (service) {
                    service.getCoordinator().receive(message);
                }
            }
        });
    }

    @Override
    public void register(ISerializationRegistry registry) {
        registry.register(new ActionMessageSerializer());
        registry.register(new ActionResponseMessageSerializer());
        registry.register(new InstallMessageSerializer());
        registry.register(new InstallRoleMessageSerializer());
        registry.register(new ControlMessageSerializer());
        registry.register(new ResponseMessageSerializer());
        registry.register(new SynchronizeRolesMessageSerializer());
        registry.register(new SynchronizeRolesResponseMessageSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry) {
        registry.unregister(ActionMessageSerializer.ID);
        registry.unregister(ActionResponseMessageSerializer.ID);
        registry.unregister(InstallMessageSerializer.ID);
        registry.unregister(InstallRoleMessageSerializer.ID);
        registry.unregister(ControlMessageSerializer.ID);
        registry.unregister(ResponseMessageSerializer.ID);
        registry.unregister(SynchronizeRolesMessageSerializer.ID);
        registry.unregister(SynchronizeRolesResponseMessageSerializer.ID);
    }

    public void send(IMessagePart part) {
        IMessage message = channel.getMessageFactory().create(address, part);
        channel.send(message);
    }

    public void send(IMessagePart part, List<File> files) {
        IMessage message = channel.getMessageFactory().create(address, part, 0, files);
        channel.send(message);
    }

    private void connect() {
        String agentAddress = "tcp://" + configuration.getHost() + ":" + configuration.getPort();
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.connecting(agentAddress));

        channel.connect(agentAddress);
        lastConnectTime = currentTime;
    }

    private void disconnect() {
        state = State.STARTED;
        lastConnectTime = currentTime;
        if (address != null) {
            service.removeChannel(address);
            service.getCoordinator().onNodeFailed(address);
        }

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.disconnected());
    }

    private enum State {
        STARTED,
        CONNECTED
    }

    private interface IMessages {
        @DefaultMessage("Test coordinator channel is started.")
        ILocalizedMessage started();

        @DefaultMessage("Test coordinator channel is connecting to test agent ''{0}''.")
        ILocalizedMessage connecting(String agent);

        @DefaultMessage("Test coordinator channel is connected to test agent ''{0}''.")
        ILocalizedMessage connected(IAddress agent);

        @DefaultMessage("Test coordinator channel is disconnected.")
        ILocalizedMessage disconnected();

        @DefaultMessage("Test coordinator channel is stopped.")
        ILocalizedMessage stopped();
    }
}
