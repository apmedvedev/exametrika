/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.exametrika.api.agent.config.TransportConfiguration;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.profiler.IProfilingService;
import com.exametrika.api.server.IServerService;
import com.exametrika.api.server.config.ServerConfiguration;
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
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.spi.aggregator.IMeasurementRequestor;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;
import com.exametrika.spi.component.IAgentActionExecutor;
import com.exametrika.spi.component.IAgentFailureDetector;
import com.exametrika.spi.component.IAgentSchemaUpdater;
import com.exametrika.spi.profiler.IProfilerMeasurementHandler;


/**
 * The {@link ServerService} represents a server service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ServerService implements IService, IServiceProvider, IServerService, IProfilerMeasurementHandler {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ServerService.class);
    private ServerConfiguration configuration;
    private volatile ServerChannel channel;
    private final AgentFailureDetector agentFailureDetector = new AgentFailureDetector();
    private final AgentActionExecutor agentActionExecutor = new AgentActionExecutor();
    private final ServerPlatformUpdater platformUpdater = new ServerPlatformUpdater(this);
    private final MeasurementRequestor measurementRequestor = new MeasurementRequestor();
    private IDatabase database;
    private volatile ServerProfilerMeasurementSender profilerMeasurementSender;
    private volatile IAggregationSchema schema;

    public ServerChannel getChannel() {
        return channel;
    }

    @Override
    public IDatabase getDatabase() {
        return database;
    }

    @Override
    public void register(IServiceRegistrar registrar) {
        registrar.register(NAME, this);
    }

    @Override
    public void wire(IServiceRegistry registry) {
        IProfilingService profilingService = registry.findService(IProfilingService.NAME);
        if (profilingService != null)
            profilingService.setMeasurementHandler(this);
    }

    @Override
    public synchronized void start(IServiceRegistry registry) {
        Assert.notNull(registry);

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.started());
    }

    @Override
    public synchronized void stop(boolean fromShutdownHook) {
        destroyChannel();
        IOs.close(database);
        database = null;
        configuration = null;
        profilerMeasurementSender = null;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.stopped());
    }

    @Override
    public synchronized void setConfiguration(ILoadContext context) {
        ServerConfiguration configuration = context.get(ServerConfiguration.SCHEMA);
        Assert.notNull(configuration);

        boolean created = false;
        if (this.configuration == null || !Objects.equals(configuration.getChannel(), this.configuration.getChannel())) {
            destroyChannel();
            this.configuration = configuration;
            createChannel();
            created = true;
        } else
            this.configuration = configuration;

        if (database == null)
            createDatabase(configuration.getDatabase());
        else
            database.setConfiguration(configuration.getDatabase());

        if (created)
            channel.start();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.configurationUpdated());
    }

    @Override
    public void onTimer(long currentTime) {
        ServerChannel channel = this.channel;
        if (channel != null)
            channel.onTimer(currentTime);

        ServerProfilerMeasurementSender profilerMeasurementSender = this.profilerMeasurementSender;
        if (profilerMeasurementSender != null)
            profilerMeasurementSender.onTimer(currentTime);
    }

    @Override
    public boolean canHandle() {
        return true;
    }

    @Override
    public void handle(MeasurementSet measurements) {
        ServerProfilerMeasurementSender profilerMeasurementSender = this.profilerMeasurementSender;
        if (profilerMeasurementSender != null)
            profilerMeasurementSender.handle(measurements);
    }

    @Override
    public void setSchema(IAggregationSchema schema) {
        ServerProfilerMeasurementSender profilerMeasurementSender = this.profilerMeasurementSender;
        if (profilerMeasurementSender != null)
            profilerMeasurementSender.setSchema(schema);

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

        ServerChannel serverChannel = new ServerChannel(configuration, agentFailureDetector, agentActionExecutor,
                platformUpdater, measurementRequestor);

        ChannelParameters parameters = new ChannelParameters();
        parameters.channelName = configuration.getName();
        parameters.serverPart = true;
        parameters.portRangeStart = configuration.getChannel().getPort();
        parameters.portRangeEnd = configuration.getChannel().getPort();
        parameters.receiver = serverChannel;

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

        if (configuration.getChannel().getMaxTotalRate() != null) {
            int maxRate = configuration.getChannel().getMaxTotalRate();
            parameters.rateController = new TcpRateController(true, maxRate / 2, maxRate, true,
                    maxRate / 2, maxRate, true, 1000, 100);
        }
        parameters.serializationRegistrars.add(serverChannel);

        IChannel channel = factory.createChannel(parameters);
        serverChannel.setChannel(channel);
        channel.getChannelObserver().addChannelListener(serverChannel);
        agentActionExecutor.setChannel(channel);
        agentFailureDetector.setChannel(channel);
        platformUpdater.setChannel(channel);
        measurementRequestor.setChannel(channel);
        measurementRequestor.setServerChannel(serverChannel);

        this.channel = serverChannel;
    }

    private void destroyChannel() {
        if (channel != null) {
            channel.stop();
            channel = null;
        }
    }

    private void createDatabase(DatabaseConfiguration configuration) {
        IDatabaseFactory.Parameters parameters = new IDatabaseFactory.Parameters();
        parameters.parameters.put(IAgentActionExecutor.NAME, agentActionExecutor);
        parameters.parameters.put(IAgentFailureDetector.NAME, agentFailureDetector);
        parameters.parameters.put(IAgentSchemaUpdater.NAME, platformUpdater);
        parameters.parameters.put(IMeasurementRequestor.NAME, measurementRequestor);

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        channel.setDatabase(database);
        platformUpdater.setDatabase(database);

        database.open();

        profilerMeasurementSender = new ServerProfilerMeasurementSender(database, schema);
    }

    private interface IMessages {
        @DefaultMessage("Server service is started.")
        ILocalizedMessage started();

        @DefaultMessage("Server service is stopped.")
        ILocalizedMessage stopped();

        @DefaultMessage("Configuration of server service is updated.")
        ILocalizedMessage configurationUpdated();
    }
}
