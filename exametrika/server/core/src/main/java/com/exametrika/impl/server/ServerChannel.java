/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.component.config.model.ComponentModelSchemaConfiguration;
import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.api.server.config.ServerConfiguration;
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
import com.exametrika.impl.agent.messages.ActionMessageSerializer;
import com.exametrika.impl.agent.messages.AgentStartMessageSerializer;
import com.exametrika.impl.agent.messages.MeasurementsMessageSerializer;
import com.exametrika.impl.agent.messages.PlatformUpdateMessageSerializer;
import com.exametrika.impl.agent.messages.RemoveNamesMessageSerializer;
import com.exametrika.impl.agent.messages.RequestMeasurementsMessageSerializer;
import com.exametrika.impl.agent.messages.ResetDictionaryMessageSerializer;
import com.exametrika.impl.agent.messages.TimeSynchronizationMessageSerializer;


/**
 * The {@link ServerChannel} represents a server channel.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ServerChannel implements IReceiver, IChannelListener, ISerializationRegistrar,
        ILifecycle, ITimeService {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ServerChannel.class);
    private final IMarker marker;
    private final long channelTimeout;
    private boolean started;
    private IChannel channel;
    private IDatabase database;
    private long currentTime;
    private Map<IAddress, ServerAgentChannel> agents = new LinkedHashMap<IAddress, ServerAgentChannel>();
    private long lastCleanupTime;
    private final AgentFailureDetector agentFailureDetector;
    private final AgentActionExecutor agentActionExecutor;
    private final ServerPlatformUpdater platformUpdater;
    private final MeasurementRequestor measurementRequestor;

    public ServerChannel(ServerConfiguration configuration, AgentFailureDetector agentFailureDetector,
                         AgentActionExecutor agentActionExecutor, ServerPlatformUpdater platformUpdater,
                         MeasurementRequestor measurementRequestor) {
        Assert.notNull(agentFailureDetector);
        Assert.notNull(agentActionExecutor);
        Assert.notNull(platformUpdater);
        Assert.notNull(configuration);
        Assert.notNull(measurementRequestor);

        this.agentFailureDetector = agentFailureDetector;
        this.agentActionExecutor = agentActionExecutor;
        this.platformUpdater = platformUpdater;
        this.measurementRequestor = measurementRequestor;
        this.channelTimeout = configuration.getChannel().getTransport() != null ? configuration.getChannel(
        ).getTransport().getTransportChannelTimeout() : 60000;
        marker = Loggers.getMarker(configuration.getName());
    }

    public IChannel getChannel() {
        return channel;
    }

    public void setChannel(IChannel channel) {
        Assert.notNull(channel);
        Assert.isNull(this.channel);

        this.channel = channel;
    }

    public void setDatabase(IDatabase database) {
        Assert.notNull(database);
        Assert.isNull(this.database);

        this.database = database;
    }

    public synchronized Set<IAddress> getAgents() {
        return new HashSet<IAddress>(agents.keySet());
    }

    public void onTimer(long currentTime) {
        List<ServerAgentChannel> disconnectingChannels = null;
        synchronized (this) {
            this.currentTime = currentTime;

            if (lastCleanupTime != 0 && currentTime - lastCleanupTime < 1000)
                return;

            lastCleanupTime = currentTime;

            for (ServerAgentChannel agent : agents.values()) {
                if (!agent.isConnected() && (currentTime - agent.getCreateTime()) > channelTimeout) {
                    if (disconnectingChannels == null)
                        disconnectingChannels = new ArrayList<ServerAgentChannel>();

                    disconnectingChannels.add(agent);
                }
            }
        }

        if (disconnectingChannels != null) {
            for (ServerAgentChannel channel : disconnectingChannels)
                channel.disconnect();
        }

        measurementRequestor.onTimer(currentTime);
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
        Map<IAddress, ServerAgentChannel> agents;
        synchronized (this) {
            if (!started)
                return;

            started = false;
            agents = new LinkedHashMap<IAddress, ServerAgentChannel>(this.agents);
            this.agents.clear();
        }

        agentFailureDetector.disableAgentFailures(true);

        channel.stop();

        for (ServerAgentChannel agent : agents.values())
            agent.close();

        agentFailureDetector.disableAgentFailures(false);

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.stopped());
    }

    @Override
    public synchronized void onNodeConnected(IAddress node) {
        Assert.isTrue(!agents.containsKey(node));
        ServerAgentChannel agent = new ServerAgentChannel(database, channel, node, this, marker, agentFailureDetector,
                agentActionExecutor, platformUpdater, measurementRequestor);
        agents.put(node, agent);
    }

    @Override
    public void onNodeFailed(IAddress node) {
        ServerAgentChannel agent;
        synchronized (this) {
            agent = agents.remove(node);
        }
        if (agent != null)
            agent.close();
    }

    @Override
    public void onNodeDisconnected(IAddress node) {
        onNodeFailed(node);
    }

    @Override
    public void receive(IMessage message) {
        ServerAgentChannel agent;
        synchronized (this) {
            agent = agents.get(message.getSource());
        }
        if (agent != null)
            agent.receive(message);
    }

    @Override
    public void register(ISerializationRegistry registry) {
        registry.register(new ActionMessageSerializer());
        registry.register(new AgentStartMessageSerializer());
        registry.register(new MeasurementsMessageSerializer());
        registry.register(new RemoveNamesMessageSerializer());
        registry.register(new ResetDictionaryMessageSerializer());
        registry.register(new PlatformUpdateMessageSerializer());
        registry.register(new TimeSynchronizationMessageSerializer());
        registry.register(new RequestMeasurementsMessageSerializer());
    }

    @Override
    public void unregister(ISerializationRegistry registry) {
        registry.unregister(ActionMessageSerializer.ID);
        registry.unregister(AgentStartMessageSerializer.ID);
        registry.unregister(MeasurementsMessageSerializer.ID);
        registry.unregister(RemoveNamesMessageSerializer.ID);
        registry.unregister(ResetDictionaryMessageSerializer.ID);
        registry.unregister(PlatformUpdateMessageSerializer.ID);
        registry.unregister(TimeSynchronizationMessageSerializer.ID);
        registry.unregister(RequestMeasurementsMessageSerializer.ID);
    }

    @Override
    public long getCurrentTime() {
        return currentTime;
    }

    public void onSchemaChanged(ComponentModelSchemaConfiguration newSchema) {
        Map<IAddress, ServerAgentChannel> agents;
        synchronized (this) {
            agents = new LinkedHashMap<IAddress, ServerAgentChannel>(this.agents);
        }

        for (ServerAgentChannel agent : agents.values())
            agent.onSchemaChanged(newSchema);
    }

    private interface IMessages {
        @DefaultMessage("Server channel is started.")
        ILocalizedMessage started();

        @DefaultMessage("Server channel is stopped.")
        ILocalizedMessage stopped();
    }
}
