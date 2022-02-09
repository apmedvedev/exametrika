/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server;

import com.exametrika.api.component.config.model.ComponentModelSchemaConfiguration;
import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.agent.messages.ActionMessage;
import com.exametrika.impl.agent.messages.AgentStartMessage;
import com.exametrika.impl.agent.messages.MeasurementsMessage;
import com.exametrika.impl.agent.messages.RemoveNamesMessage;
import com.exametrika.impl.agent.messages.TimeSynchronizationMessage;


/**
 * The {@link ServerAgentChannel} represents an agent channel on server.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ServerAgentChannel {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ServerAgentChannel.class);
    private final MeasurementReceiver measurementReceiver;
    private final IAddress node;
    private final IMarker marker;
    private final IChannel channel;
    private final AgentFailureDetector agentFailureDetector;
    private final AgentActionExecutor agentActionExecutor;
    private final ServerPlatformUpdater platformUpdater;
    private final MeasurementRequestor measurementRequestor;
    private volatile String component;
    private String configHash;
    private volatile State state;
    private volatile long createTime;

    public ServerAgentChannel(IDatabase database, IChannel channel, IAddress node, ITimeService timeService, IMarker parentMarker,
                              AgentFailureDetector agentFailureDetector, AgentActionExecutor agentActionExecutor, ServerPlatformUpdater platformUpdater,
                              MeasurementRequestor measurementRequestor) {
        Assert.notNull(database);
        Assert.notNull(channel);
        Assert.notNull(node);
        Assert.notNull(timeService);
        Assert.notNull(agentFailureDetector);
        Assert.notNull(agentActionExecutor);
        Assert.notNull(platformUpdater);
        Assert.notNull(measurementRequestor);

        this.channel = channel;
        this.node = node;
        marker = Loggers.getMarker("connection:" + node, parentMarker);
        this.measurementReceiver = new ChannelMeasurementReceiver(database, marker, this, channel, measurementRequestor);
        this.agentFailureDetector = agentFailureDetector;
        this.agentActionExecutor = agentActionExecutor;
        this.platformUpdater = platformUpdater;
        this.measurementRequestor = measurementRequestor;

        state = State.STARTED;
        createTime = timeService.getCurrentTime();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.started());
    }

    public IAddress getNode() {
        return node;
    }

    public boolean isConnected() {
        return state == State.CONNECTED;
    }

    public long getCreateTime() {
        return createTime;
    }

    public IMarker getMarker() {
        return marker;
    }

    public void disconnect() {
        channel.disconnect(node.getConnection(0));
    }

    public void close() {
        synchronized (this) {
            if (state == null)
                return;

            state = null;
        }

        agentFailureDetector.fireOnAgentFailed(node.getName());
        agentActionExecutor.onAgentFailed(node.getName());
        measurementRequestor.onAgentFailed(node);
        measurementReceiver.reset();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.stopped());
    }

    public void receive(IMessage message) {
        if (message.getPart() instanceof TimeSynchronizationMessage) {
            TimeSynchronizationMessage part = message.getPart();
            IMessage response = channel.getMessageFactory().create(node, new TimeSynchronizationMessage(
                    part.getAgentTime(), Times.getCurrentTime()));
            channel.send(response);
        } else if (message.getPart() instanceof AgentStartMessage) {
            AgentStartMessage part = message.getPart();
            component = part.getComponent();
            if (configHash == null && part.getConfigHash() != null)
                configHash = part.getConfigHash();
            state = State.CONNECTED;
            platformUpdater.receive(this, part);
            agentFailureDetector.fireOnAgentActivated(message.getSource().getName());
        } else if (state == State.CONNECTED && message.getPart() instanceof MeasurementsMessage)
            measurementReceiver.receive((MeasurementsMessage) message.getPart());
        else if (state == State.CONNECTED && message.getPart() instanceof RemoveNamesMessage)
            measurementReceiver.receive((RemoveNamesMessage) message.getPart());
        else if (state == State.CONNECTED && message.getPart() instanceof ActionMessage)
            agentActionExecutor.receive(node.getName(), (ActionMessage) message.getPart());
    }

    public void onSchemaChanged(ComponentModelSchemaConfiguration newSchema) {
        if (state != State.CONNECTED)
            return;

        ComponentSchemaConfiguration component = newSchema.findComponent(this.component);
        platformUpdater.updatePlatform(this, component, this.component, configHash);
    }

    public void setConfigHash(String value) {
        this.configHash = value;
    }

    private enum State {
        STARTED,
        CONNECTED,
    }

    private interface IMessages {
        @DefaultMessage("Server agent channel is started.")
        ILocalizedMessage started();

        @DefaultMessage("Server agent channel is stopped.")
        ILocalizedMessage stopped();
    }
}
