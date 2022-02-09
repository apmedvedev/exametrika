/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.monitors;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.tasks.impl.RunnableTaskHandler;
import com.exametrika.common.tasks.impl.TaskExecutor;
import com.exametrika.common.tasks.impl.TaskQueue;
import com.exametrika.common.tasks.impl.Timer;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.profiler.ProfilingService;
import com.exametrika.impl.profiler.strategies.MeasurementStrategyManager;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;
import com.exametrika.spi.profiler.IDumpProvider;
import com.exametrika.spi.profiler.IMeasurementStrategy;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;


/**
 * The {@link MonitorManager} is a manager of monitors.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MonitorManager implements ITimerListener {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(MonitorManager.class);
    private final MeasurementStrategyManager measurementStrategyManager;
    private final Timer timer;
    private ProfilerConfiguration configuration;
    private final TaskExecutor<Runnable> taskExecutor;
    private final TaskQueue<Runnable> tasks;
    private final MonitorContext context;
    private volatile List<MonitorInfo> monitors = new ArrayList<MonitorInfo>();
    private boolean started;
    private boolean stopped;
    private long nextFullExtractionTime;

    public MonitorManager(MeasurementStrategyManager measurementStrategyManager, IMeasurementHandler measurementHandler,
                          ITimeService timeService, Map<String, String> agentArgs, ProfilingService profilingService) {
        Assert.notNull(measurementStrategyManager);
        Assert.notNull(measurementHandler);
        Assert.notNull(timeService);

        this.measurementStrategyManager = measurementStrategyManager;
        tasks = new TaskQueue<Runnable>(1000, 0);
        taskExecutor = new TaskExecutor<Runnable>(Runtime.getRuntime().availableProcessors() * 2, tasks, new RunnableTaskHandler(),
                "Monitor manager task thread");
        timer = new Timer(100, this, false, "Monitor manager timer thread", null);
        ProfilerConfiguration configuration = new ProfilerConfiguration("node", TimeSource.WALL_TIME,
                Collections.<MeasurementStrategyConfiguration>emptySet(),
                Collections.<ScopeConfiguration>emptySet(), Collections.<MonitorConfiguration>emptySet(),
                Collections.<ProbeConfiguration>emptySet(), 1, 1, 100, 300000, 300000, new File(""), 100000, Enums.noneOf(DumpType.class),
                60000, JsonUtils.EMPTY_OBJECT, null);
        context = new MonitorContext(configuration, timeService, measurementHandler, tasks, agentArgs, profilingService);
    }

    public synchronized void setConfiguration(ProfilerConfiguration configuration) {
        Assert.checkState(started && !stopped);

        if (configuration != null) {
            context.setConfiguration(configuration);
            taskExecutor.setThreadCount(configuration.getMonitorThreadPoolSize());
            timer.setPeriod(configuration.getMonitorTimerPeriod());
        }

        if (this.configuration == null) {
            List<MonitorInfo> monitors = new ArrayList<MonitorInfo>();
            for (MonitorConfiguration monitor : configuration.getMonitors())
                monitors.add(createMonitor(monitor));

            this.monitors = monitors;
        } else if (configuration == null) {
            for (MonitorInfo info : monitors)
                stopMonitor(info);

            monitors = new ArrayList<MonitorInfo>();
        } else {
            Map<String, MonitorConfiguration> oldMonitorsMap = new LinkedHashMap<String, MonitorConfiguration>();
            for (MonitorConfiguration monitor : this.configuration.getMonitors())
                oldMonitorsMap.put(monitor.getName(), monitor);

            Map<String, MonitorConfiguration> newMonitorsMap = new LinkedHashMap<String, MonitorConfiguration>();
            for (MonitorConfiguration monitor : configuration.getMonitors())
                newMonitorsMap.put(monitor.getName(), monitor);

            Set<MonitorConfiguration> newMonitors = new LinkedHashSet<MonitorConfiguration>();
            Set<String> removedMonitors = new LinkedHashSet<String>();

            for (MonitorConfiguration monitor : this.configuration.getMonitors()) {
                MonitorConfiguration newMonitor = newMonitorsMap.get(monitor.getName());
                if (newMonitor == null)
                    removedMonitors.add(monitor.getName());
                else if (!monitor.equals(newMonitor)) {
                    removedMonitors.add(monitor.getName());
                    newMonitors.add(newMonitor);
                }
            }

            for (MonitorConfiguration monitor : configuration.getMonitors()) {
                if (!oldMonitorsMap.containsKey(monitor.getName()))
                    newMonitors.add(monitor);
            }

            List<MonitorInfo> monitors = new ArrayList<MonitorInfo>();
            for (MonitorInfo info : this.monitors) {
                if (!removedMonitors.contains(info.configuration.getName())) {
                    if (info.configuration.getMeasurementStrategy() != null) {
                        info.measurementStrategy = measurementStrategyManager.findMeasurementStrategy(
                                info.configuration.getMeasurementStrategy());
                        Assert.checkState(info.measurementStrategy != null);
                    }
                    monitors.add(info);
                } else
                    stopMonitor(info);
            }

            for (MonitorConfiguration monitor : newMonitors)
                monitors.add(createMonitor(monitor));

            this.monitors = monitors;
        }

        this.configuration = configuration;
    }

    @Override
    public void onTimer() {
        if (!context.getMeasurementHandler().canHandle())
            return;

        long currentTime = context.getTimeService().getCurrentTime();
        boolean force = false;

        ProfilerConfiguration configuration = this.configuration;
        if (configuration != null) {
            if (nextFullExtractionTime > 0 && currentTime >= nextFullExtractionTime)
                force = true;

            nextFullExtractionTime = (currentTime / configuration.getFullExtractionPeriod() + 1) *
                    configuration.getFullExtractionPeriod();
        }

        List<MonitorInfo> monitors = this.monitors;

        int schemaVersion = context.getConfiguration().getSchemaVersion();
        List<Measurement> measurements = new ArrayList<Measurement>();
        for (MonitorInfo info : monitors) {
            if (!info.canMeasure(currentTime, force))
                continue;

            try {
                long period = (info.lastMeasurementTime != 0) ? currentTime - info.lastMeasurementTime : info.configuration.getPeriod();
                info.monitor.measure(measurements, currentTime, period, force);
                info.lastMeasurementTime = currentTime;
            } catch (Exception e) {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }
        }

        if (!measurements.isEmpty()) {
            MeasurementSet set = new MeasurementSet(measurements, null, schemaVersion, currentTime, 0);
            context.getMeasurementHandler().handle(set);
        }
    }

    public synchronized void start() {
        Assert.checkState(!started);

        taskExecutor.start();
        timer.start();

        started = true;
    }

    public synchronized void stop() {
        Assert.checkState(started && !stopped);

        timer.stop();
        taskExecutor.stop();

        setConfiguration(null);
        stopped = true;
    }

    public void dump(Json json, int flags) {
        Json monitorsDump = json.putObject("monitors");
        for (MonitorInfo info : monitors) {
            if (!(info.monitor instanceof IDumpProvider))
                continue;

            IDumpProvider dumpProvider = (IDumpProvider) info.monitor;
            monitorsDump.put(dumpProvider.getName(), dumpProvider.dump(flags));
        }
    }

    private MonitorInfo createMonitor(MonitorConfiguration monitor) {
        IMeasurementStrategy measurementStrategy = null;
        if (monitor.getMeasurementStrategy() != null) {
            measurementStrategy = measurementStrategyManager.findMeasurementStrategy(monitor.getMeasurementStrategy());
            Assert.checkState(measurementStrategy != null);
        }

        MonitorInfo info = new MonitorInfo(monitor, monitor.createMonitor(context), measurementStrategy);
        startMonitor(info);
        return info;
    }

    private static class MonitorInfo {
        private final MonitorConfiguration configuration;
        private final IMonitor monitor;
        private long nextMeasurementTime;
        private long lastMeasurementTime;
        private volatile IMeasurementStrategy measurementStrategy;

        public MonitorInfo(MonitorConfiguration configuration, IMonitor monitor, IMeasurementStrategy measurementStrategy) {
            this.configuration = configuration;
            this.monitor = monitor;
            this.measurementStrategy = measurementStrategy;
        }

        public boolean canMeasure(long currentTime, boolean force) {
            if (!force && nextMeasurementTime > 0 && currentTime < nextMeasurementTime)
                return false;

            IMeasurementStrategy measurementStrategy = this.measurementStrategy;
            if (measurementStrategy != null && !measurementStrategy.allow())
                return false;

            boolean first = nextMeasurementTime == 0;
            nextMeasurementTime = (currentTime / configuration.getPeriod() + 1) * configuration.getPeriod();
            return !first;
        }
    }

    private void startMonitor(MonitorInfo info) {
        info.monitor.start();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.monitorStarted(info.configuration.getName()));
    }

    private void stopMonitor(MonitorInfo info) {
        info.monitor.stop();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.monitorStopped(info.configuration.getName()));
    }

    private interface IMessages {
        @DefaultMessage("Monitor ''{0}'' is started.")
        ILocalizedMessage monitorStarted(String name);

        @DefaultMessage("Monitor ''{0}'' is stopped.")
        ILocalizedMessage monitorStopped(String name);
    }
}
