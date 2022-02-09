/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server;

import java.util.Collections;

import com.exametrika.api.component.IComponentService;
import com.exametrika.api.component.config.model.AgentComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.ComponentModelSchemaConfiguration;
import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.component.config.schema.ComponentServiceSchemaConfiguration;
import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IMessage;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.agent.messages.AgentStartMessage;
import com.exametrika.impl.agent.messages.PlatformUpdateMessage;
import com.exametrika.spi.component.IAgentSchemaUpdater;
import com.exametrika.spi.exadb.core.IDomainService;


/**
 * The {@link ServerPlatformUpdater} is a server part of platform updater.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ServerPlatformUpdater implements IAgentSchemaUpdater {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ServerService.class);
    private final ServerService serverService;
    private IDatabase database;
    private IChannel channel;
    private IDomainService componentService;

    public ServerPlatformUpdater(ServerService serverService) {
        Assert.notNull(serverService);

        this.serverService = serverService;
    }

    public void setDatabase(IDatabase database) {
        Assert.notNull(database);

        this.database = database;
    }

    public void setChannel(IChannel channel) {
        Assert.notNull(channel);

        this.channel = channel;
    }

    public void receive(final ServerAgentChannel agent, final AgentStartMessage part) {
        Assert.checkState(channel != null && database != null);

        database.transaction(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                receiveInTransaction(transaction, agent, part);
            }
        });
    }

    @Override
    public void onSchemaChanged(ComponentModelSchemaConfiguration newSchema) {
        ServerChannel channel = serverService.getChannel();
        if (channel != null)
            channel.onSchemaChanged(newSchema);
    }

    public void updatePlatform(ServerAgentChannel agent, ComponentSchemaConfiguration component, String componentName, String agentConfigHash) {
        String configHash = null;
        String profilerConfiguration = null;
        if (component instanceof AgentComponentSchemaConfiguration) {
            AgentComponentSchemaConfiguration agentSchema = (AgentComponentSchemaConfiguration) component;
            configHash = Strings.md5Hash(Collections.singletonList(agentSchema.getProfilerConfiguration()));
            if (configHash.equals(agentConfigHash))
                configHash = null;
            else {
                profilerConfiguration = agentSchema.getProfilerConfiguration();
                agent.setConfigHash(configHash);
            }

            IMessage message = channel.getMessageFactory().create(agent.getNode(), new PlatformUpdateMessage(profilerConfiguration, configHash, null));
            channel.send(message);
        } else {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, agent.getMarker(), messages.componentNotFound(componentName));

            agent.disconnect();
        }
    }

    private void receiveInTransaction(ITransaction transaction, ServerAgentChannel agent, AgentStartMessage part) {
        ensureComponentService(transaction);

        ComponentModelSchemaConfiguration componentModel =
                ((ComponentServiceSchemaConfiguration) componentService.getSchema().getConfiguration()).getComponentModel();
        ComponentSchemaConfiguration component = componentModel.findComponent(part.getComponent());
        updatePlatform(agent, component, part.getComponent(), part.getConfigHash());
    }

    private void ensureComponentService(ITransaction transaction) {
        if (componentService != null)
            return;

        componentService = transaction.findDomainService(IComponentService.NAME);
    }

    private interface IMessages {
        @DefaultMessage("Component ''{0}'' is not found in component model of server. Connection rejected.")
        ILocalizedMessage componentNotFound(String componentName);
    }
}
