/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.monitors;

import java.util.Map;

import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.tasks.ITaskQueue;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.profiler.ProfilingService;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;
import com.exametrika.spi.profiler.IMonitorContext;


/**
 * The {@link MonitorContext} is a runtime context for monitors.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MonitorContext implements IMonitorContext, IInstanceContextProvider {
    private final ITimeService timeService;
    private final IMeasurementHandler measurementHandler;
    private final ITaskQueue<Runnable> taskQueue;
    private final ThreadLocal<JsonObject> context = new ThreadLocal<JsonObject>();
    private volatile ProfilerConfiguration configuration;
    private final Map<String, String> agentArgs;
    private ProfilingService profilingService;

    public MonitorContext(ProfilerConfiguration configuration, ITimeService timeService, IMeasurementHandler measurementHandler,
                          ITaskQueue<Runnable> taskQueue, Map<String, String> agentArgs, ProfilingService profilingService) {
        Assert.notNull(configuration);
        Assert.notNull(timeService);
        Assert.notNull(measurementHandler);
        Assert.notNull(taskQueue);
        Assert.notNull(agentArgs);

        this.configuration = configuration;
        this.timeService = timeService;
        this.measurementHandler = measurementHandler;
        this.taskQueue = taskQueue;
        this.agentArgs = agentArgs;
        this.profilingService = profilingService;
    }

    public ProfilingService getProfilingService() {
        return profilingService;
    }

    public void setConfiguration(ProfilerConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    public Map<String, String> getAgentArgs() {
        return agentArgs;
    }

    @Override
    public ProfilerConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public int getSchemaVersion() {
        return configuration.getSchemaVersion();
    }

    @Override
    public ITimeService getTimeService() {
        return timeService;
    }

    @Override
    public IMeasurementHandler getMeasurementHandler() {
        return measurementHandler;
    }

    @Override
    public JsonObject getInstanceContext() {
        return context.get();
    }

    @Override
    public void setInstanceContext(JsonObject value) {
        context.set(value);
    }

    @Override
    public ITaskQueue<Runnable> getTaskQueue() {
        return taskQueue;
    }

    @Override
    public JsonObject getContext() {
        return context.get();
    }

    @Override
    public void setContext(JsonObject value) {
        context.set(value);
    }

    @Override
    public long getExtractionTime() {
        return timeService.getCurrentTime();
    }
}
