/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.agent;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.exametrika.api.tester.config.TestAgentConfiguration;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.ChannelParameters;
import com.exametrika.common.services.IService;
import com.exametrika.common.services.IServiceProvider;
import com.exametrika.common.services.IServiceRegistrar;
import com.exametrika.common.services.IServiceRegistry;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link TestAgentService} represents a test agent service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestAgentService implements IService, IServiceProvider {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(TestAgentService.class);
    private TestAgentConfiguration configuration;
    private volatile TestAgentChannel channel;
    private boolean stopped;

    @Override
    public void register(IServiceRegistrar registrar) {
        registrar.register("testAgent", this);
    }

    @Override
    public void wire(IServiceRegistry registry) {
    }

    @Override
    public synchronized void start(IServiceRegistry registry) {
        Assert.notNull(registry);

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.started());
    }

    @Override
    public void stop(boolean fromShutdownHook) {
        synchronized (this) {
            if (stopped)
                return;

            stopped = true;
        }

        destroyChannel();
        configuration = null;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.stopped());
    }

    @Override
    public synchronized void setConfiguration(ILoadContext context) {
        TestAgentConfiguration configuration = context.get(TestAgentConfiguration.SCHEMA);
        Assert.notNull(configuration);

        if (Objects.equals(configuration, this.configuration))
            return;

        destroyChannel();

        this.configuration = configuration;

        if (configuration != null)
            createChannel();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.configurationUpdated());
    }

    @Override
    public void onTimer(long currentTime) {
        TestAgentChannel channel = this.channel;
        if (channel != null)
            channel.onTimer(currentTime);
    }

    private void createChannel() {
        ChannelFactory factory = new ChannelFactory();
        TestAgentChannel agentChannel = new TestAgentChannel(configuration);

        ChannelParameters parameters = new ChannelParameters();
        parameters.channelName = configuration.getName();
        parameters.serverPart = true;
        parameters.portRangeStart = configuration.getChannel().getPort();
        parameters.portRangeEnd = configuration.getChannel().getPort();
        parameters.receiver = agentChannel;

        if (configuration.getChannel().getBindAddress() != null) {
            try {
                parameters.bindAddress = InetAddress.getByName(configuration.getChannel().getBindAddress());
            } catch (UnknownHostException e) {
                throw new ChannelException(e);
            }
        }

        parameters.secured = configuration.getChannel().isSecured();
        parameters.keyStorePath = configuration.getChannel().getKeyStorePath();
        parameters.keyStorePassword = configuration.getChannel().getKeyStorePassword();

        parameters.serializationRegistrars.add(agentChannel);

        IChannel channel = factory.createChannel(parameters);
        agentChannel.setChannel(channel);
        channel.getChannelObserver().addChannelListener(agentChannel);

        this.channel = agentChannel;
        agentChannel.start();
    }

    private void destroyChannel() {
        if (channel != null) {
            channel.stop();
            channel = null;
        }
    }

    private interface IMessages {
        @DefaultMessage("Test agent service is started.")
        ILocalizedMessage started();

        @DefaultMessage("Test agent service is stopped.")
        ILocalizedMessage stopped();

        @DefaultMessage("Configuration of test agent service is updated.")
        ILocalizedMessage configurationUpdated();
    }
}
