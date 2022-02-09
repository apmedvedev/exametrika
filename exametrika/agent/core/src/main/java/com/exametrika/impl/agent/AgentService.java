/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import com.exametrika.api.agent.config.AgentConfiguration;
import com.exametrika.api.agent.config.TransportConfiguration;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.profiler.IProfilingService;
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
import com.exametrika.common.messaging.impl.ChannelFactoryParameters;
import com.exametrika.common.messaging.impl.ChannelParameters;
import com.exametrika.common.net.utils.TcpRateController;
import com.exametrika.common.services.IService;
import com.exametrika.common.services.IServiceProvider;
import com.exametrika.common.services.IServiceRegistrar;
import com.exametrika.common.services.IServiceRegistry;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.boot.utils.IHotDeployer;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;
import com.exametrika.spi.profiler.IProfilerMeasurementHandler;


/**
 * The {@link AgentService} represents an agent service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AgentService implements IService, IServiceProvider, IProfilerMeasurementHandler {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(AgentService.class);
    private IServiceRegistry serviceRegistry;
    private AgentConfiguration configuration;
    private IProfilingService profilingService;
    private volatile AgentChannel channel;
    private IAggregationSchema schema;

    @Override
    public void register(IServiceRegistrar registrar) {
        registrar.register("agent", this);
    }

    @Override
    public void wire(IServiceRegistry registry) {
        Map<String, String> agentArgs = registry.findParameter("agentArgs");
        if (!agentArgs.containsKey("calibrate")) {
            profilingService = registry.findService(IProfilingService.NAME);
            Assert.checkState(profilingService != null);

            profilingService.setMeasurementHandler(this);
        }
    }

    @Override
    public synchronized void start(IServiceRegistry registry) {
        Assert.notNull(registry);

        this.serviceRegistry = registry;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.started());
    }

    @Override
    public synchronized void stop(boolean fromShutdownHook) {
        destroyChannel();

        IProfilingService profilingService = serviceRegistry.findService(IProfilingService.NAME);
        Assert.checkState(profilingService != null);

        profilingService.setMeasurementHandler(null);

        configuration = null;
        serviceRegistry = null;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.stopped());
    }

    @Override
    public synchronized void setConfiguration(ILoadContext context) {
        AgentConfiguration configuration = context.get(AgentConfiguration.SCHEMA);
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
        AgentChannel channel = this.channel;
        if (channel != null)
            channel.onTimer(currentTime);
    }

    @Override
    public boolean canHandle() {
        AgentChannel channel = this.channel;
        if (channel != null)
            return channel.getMeasurementSender().canHandle();
        else
            return false;
    }

    @Override
    public void handle(MeasurementSet measurements) {
        AgentChannel channel = this.channel;
        if (channel != null)
            channel.getMeasurementSender().handle(measurements);
    }

    @Override
    public synchronized void setSchema(IAggregationSchema schema) {
        if (channel != null)
            channel.getMeasurementSender().setSchema(schema);

        this.schema = schema;
    }

    private void createChannel() {
        ChannelFactory factory;
        if (configuration.getChannel().getTransport() != null) {
            TransportConfiguration transport = configuration.getChannel().getTransport();
            ChannelFactoryParameters factoryParameters = new ChannelFactoryParameters(transport.isDebug());
            factoryParameters.selectionPeriod = transport.getSelectionPeriod();
            factoryParameters.cleanupPeriod = transport.getCleanupPeriod();
            factoryParameters.compressionLevel = transport.getCompressionLevel();
            factoryParameters.streamingMaxFragmentSize = transport.getStreamingMaxFragmentSize();
            factoryParameters.heartbeatTrackPeriod = transport.getHeartbeatTrackPeriod();
            factoryParameters.heartbeatStartPeriod = transport.getHeartbeatStartPeriod();
            factoryParameters.heartbeatPeriod = transport.getHeartbeatPeriod();
            factoryParameters.heartbeatFailureDetectionPeriod = transport.getHeartbeatFailureDetectionPeriod();
            factoryParameters.transportChannelTimeout = transport.getTransportChannelTimeout();
            factoryParameters.transportMaxChannelIdlePeriod = transport.getTransportMaxChannelIdlePeriod();
            factoryParameters.transportMaxUnlockSendQueueCapacity = transport.getTransportMaxUnlockSendQueueCapacity();
            factoryParameters.transportMinLockSendQueueCapacity = transport.getTransportMinLockSendQueueCapacity();
            factoryParameters.transportMaxPacketSize = transport.getTransportMaxPacketSize();
            factoryParameters.transportMinReconnectPeriod = transport.getTransportMinReconnectPeriod();

            factory = new ChannelFactory(factoryParameters);
        } else
            factory = new ChannelFactory();

        IHotDeployer hotDeployer = serviceRegistry.findParameter("hotDeployer");

        AgentChannel agentChannel = new AgentChannel(configuration, hotDeployer, serviceRegistry, profilingService);

        ChannelParameters parameters = new ChannelParameters();
        parameters.channelName = configuration.getName();
        parameters.clientPart = true;
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

        if (configuration.getChannel().getMaxRate() != null) {
            int maxRate = configuration.getChannel().getMaxRate();
            parameters.rateController = new TcpRateController(true, maxRate / 2, maxRate, true,
                    maxRate / 2, maxRate, false, 1000, 100);
        }
        parameters.serializationRegistrars.add(agentChannel);

        IChannel channel = factory.createChannel(parameters);
        channel.getChannelObserver().addChannelListener(agentChannel);
        agentChannel.setChannel(channel);

        if (schema != null)
            agentChannel.getMeasurementSender().setSchema(schema);

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
        @DefaultMessage("Agent service is started.")
        ILocalizedMessage started();

        @DefaultMessage("Agent service is stopped.")
        ILocalizedMessage stopped();

        @DefaultMessage("Configuration of agent service is updated.")
        ILocalizedMessage configurationUpdated();
    }
}
