/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.agent;

import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.api.tester.config.TestAgentConfiguration;
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
import com.exametrika.common.messaging.IReceiver;
import com.exametrika.common.time.ITimeService;
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
 * The {@link TestAgentChannel} represents a test agent channel.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestAgentChannel implements IReceiver, IChannelListener, ISerializationRegistrar,
        ILifecycle, ITimeService {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(TestAgentChannel.class);
    private final IMarker marker;
    private boolean started;
    private IChannel channel;
    private volatile long currentTime;
    private Map<IAddress, TestAgentCoordinatorChannel> coordinators = new LinkedHashMap<IAddress, TestAgentCoordinatorChannel>();

    public TestAgentChannel(TestAgentConfiguration configuration) {
        marker = Loggers.getMarker(configuration.getName());
    }

    public void setChannel(IChannel channel) {
        Assert.notNull(channel);
        Assert.isNull(this.channel);

        this.channel = channel;
    }

    public void onTimer(long currentTime) {
        this.currentTime = currentTime;
    }

    @Override
    public void start() {
        synchronized (this) {
            Assert.checkState(channel != null);
            Assert.checkState(!started);

            started = true;

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.started());
        }

        channel.start();
    }

    @Override
    public void stop() {
        Map<IAddress, TestAgentCoordinatorChannel> coordinators;
        synchronized (this) {
            if (!started)
                return;

            started = false;
            coordinators = this.coordinators;
            this.coordinators = new LinkedHashMap<IAddress, TestAgentCoordinatorChannel>();
        }

        channel.stop();

        for (TestAgentCoordinatorChannel coordinator : coordinators.values())
            coordinator.close();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.stopped());
    }

    @Override
    public synchronized void onNodeConnected(IAddress node) {
        Assert.isTrue(!coordinators.containsKey(node));
        Map<IAddress, TestAgentCoordinatorChannel> coordinators = new LinkedHashMap<IAddress, TestAgentCoordinatorChannel>(this.coordinators);
        TestAgentCoordinatorChannel coordinator = new TestAgentCoordinatorChannel(channel, node, marker);
        coordinators.put(node, coordinator);
        this.coordinators = coordinators;
    }

    @Override
    public void onNodeFailed(IAddress address) {
        TestAgentCoordinatorChannel coordinator;
        synchronized (this) {
            Map<IAddress, TestAgentCoordinatorChannel> coordinators = new LinkedHashMap<IAddress, TestAgentCoordinatorChannel>(this.coordinators);
            coordinator = coordinators.remove(address);
            this.coordinators = coordinators;
        }
        if (coordinator != null)
            coordinator.close();
    }

    @Override
    public void onNodeDisconnected(IAddress address) {
        onNodeFailed(address);
    }

    @Override
    public void receive(IMessage message) {
        TestAgentCoordinatorChannel coordinator = coordinators.get(message.getSource());
        if (coordinator != null)
            coordinator.receive(message);
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

    @Override
    public long getCurrentTime() {
        return currentTime;
    }

    private interface IMessages {
        @DefaultMessage("Test agent channel is started.")
        ILocalizedMessage started();

        @DefaultMessage("Test agent channel is stopped.")
        ILocalizedMessage stopped();
    }
}
