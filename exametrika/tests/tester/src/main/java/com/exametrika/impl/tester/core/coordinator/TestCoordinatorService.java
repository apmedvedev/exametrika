/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.coordinator;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.tester.config.TestAgentConnectionConfiguration;
import com.exametrika.api.tester.config.TestCoordinatorConfiguration;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.messaging.ChannelException;
import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.IChannel;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.messaging.impl.ChannelFactory;
import com.exametrika.common.messaging.impl.ChannelParameters;
import com.exametrika.common.services.IService;
import com.exametrika.common.services.IServiceProvider;
import com.exametrika.common.services.IServiceRegistrar;
import com.exametrika.common.services.IServiceRegistry;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.tester.ITestAgentDiscoveryStrategy;


/**
 * The {@link TestCoordinatorService} represents a coordinator service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestCoordinatorService implements IService, IServiceProvider {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(TestCoordinatorService.class);
    private TestCoordinatorConfiguration configuration;
    private List<TestAgentConnectionConfiguration> agents = Collections.emptyList();
    private ITestAgentDiscoveryStrategy discoveryStrategy;
    private final Map<IAddress, TestCoordinatorChannel> connectedChannels = new LinkedHashMap<IAddress, TestCoordinatorChannel>();
    private final List<TestCoordinatorChannel> channels = new ArrayList<TestCoordinatorChannel>();
    private final TestCoordinator coordinator = new TestCoordinator(this);
    private NameFilter testCaseFilter;
    private boolean stopped;

    public TestCoordinatorConfiguration getConfiguration() {
        return configuration;
    }

    public NameFilter getTestCaseFilter() {
        return testCaseFilter;
    }

    public TestCoordinator getCoordinator() {
        return coordinator;
    }

    public synchronized boolean isConnected() {
        for (TestAgentConnectionConfiguration agentConfiguration : agents) {
            boolean found = false;
            for (TestCoordinatorChannel channel : connectedChannels.values()) {
                if (channel.getConfiguration().getName().equals(agentConfiguration.getName())) {
                    found = true;
                    break;
                }
            }

            if (!found)
                return false;
        }

        return true;
    }

    public synchronized Map<IAddress, TestCoordinatorChannel> getChannels() {
        return new HashMap<IAddress, TestCoordinatorChannel>(connectedChannels);
    }

    public synchronized List<TestCoordinatorChannel> selectChannels(Map<String, String> properties) {
        Assert.notNull(properties);

        List<TestCoordinatorChannel> channels = new ArrayList<TestCoordinatorChannel>();
        for (TestCoordinatorChannel channel : connectedChannels.values()) {
            Map<String, String> channelProperties = channel.getConfiguration().getProperties();
            boolean found = true;
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                if (!entry.getValue().equals(channelProperties.get(entry.getKey()))) {
                    found = false;
                    break;
                }
            }

            if (found)
                channels.add(channel);
        }
        return channels;
    }

    public synchronized TestCoordinatorChannel findChannel(String agentName) {
        for (TestCoordinatorChannel channel : connectedChannels.values()) {
            if (channel.getConfiguration().getName().equals(agentName))
                return channel;
        }
        return null;
    }

    public synchronized List<TestAgentConnectionConfiguration> getAgentConfigurations() {
        return agents;
    }

    @Override
    public void register(IServiceRegistrar registrar) {
        registrar.register("testCoordinator", this);
    }

    @Override
    public void wire(IServiceRegistry registry) {
        Map<String, String> agentArgs = registry.findParameter("agentArgs");
        String testCaseFilter = agentArgs.get("execute");
        if (testCaseFilter != null)
            this.testCaseFilter = new NameFilter(testCaseFilter);
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

        destroyChannels();
        configuration = null;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.stopped());
    }

    @Override
    public synchronized void setConfiguration(ILoadContext context) {
        TestCoordinatorConfiguration configuration = context.get(TestCoordinatorConfiguration.SCHEMA);
        Assert.notNull(configuration);

        if (Objects.equals(configuration, this.configuration))
            return;

        destroyChannels();

        this.configuration = configuration;

        if (configuration != null)
            createChannels();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.configurationUpdated());
    }

    @Override
    public synchronized void onTimer(long currentTime) {
        for (TestCoordinatorChannel channel : channels)
            channel.onTimer(currentTime);

        coordinator.onTimer(currentTime);
    }

    public synchronized void addChannel(IAddress address, TestCoordinatorChannel testCoordinatorChannel) {
        connectedChannels.put(address, testCoordinatorChannel);
    }

    public synchronized void removeChannel(IAddress address) {
        connectedChannels.remove(address);
    }

    public synchronized void send(IAddress address, IMessagePart part) {
        TestCoordinatorChannel channel = connectedChannels.get(address);
        if (channel != null)
            channel.send(part);
    }

    public synchronized void send(IAddress address, IMessagePart part, List<File> files) {
        TestCoordinatorChannel channel = connectedChannels.get(address);
        if (channel != null)
            channel.send(part, files);
    }

    private void createChannels() {
        discoveryStrategy = configuration.getChannel().getDiscoveryStrategy().createStrategy();
        discoveryStrategy.start();
        agents = discoveryStrategy.discoverAgents();

        for (TestAgentConnectionConfiguration agent : agents)
            channels.add(createChannel(agent));
    }

    private TestCoordinatorChannel createChannel(TestAgentConnectionConfiguration agentConfiguration) {
        ChannelFactory factory = new ChannelFactory();
        TestCoordinatorChannel coordinatorChannel = new TestCoordinatorChannel(agentConfiguration, this);

        ChannelParameters parameters = new ChannelParameters();
        parameters.channelName = agentConfiguration.getName();
        parameters.clientPart = true;
        parameters.receiver = coordinatorChannel;

        if (this.configuration.getChannel().getBindAddress() != null) {
            try {
                parameters.bindAddress = InetAddress.getByName(this.configuration.getChannel().getBindAddress());
            } catch (UnknownHostException e) {
                throw new ChannelException(e);
            }
        }

        parameters.secured = this.configuration.getChannel().isSecured();
        parameters.keyStorePath = this.configuration.getChannel().getKeyStorePath();
        parameters.keyStorePassword = this.configuration.getChannel().getKeyStorePassword();

        parameters.serializationRegistrars.add(coordinatorChannel);

        IChannel channel = factory.createChannel(parameters);
        channel.getChannelObserver().addChannelListener(coordinatorChannel);
        coordinatorChannel.setChannel(channel);

        coordinatorChannel.start();

        return coordinatorChannel;
    }

    private void destroyChannels() {
        for (TestCoordinatorChannel channel : channels)
            channel.stop();

        connectedChannels.clear();
        channels.clear();

        coordinator.clear();

        agents = Collections.emptyList();
        if (discoveryStrategy != null) {
            discoveryStrategy.stop();
            discoveryStrategy = null;
        }
    }

    private interface IMessages {
        @DefaultMessage("Coordinator service is started.")
        ILocalizedMessage started();

        @DefaultMessage("Coordinator service is stopped.")
        ILocalizedMessage stopped();

        @DefaultMessage("Configuration of coordinator service is updated.")
        ILocalizedMessage configurationUpdated();
    }
}
