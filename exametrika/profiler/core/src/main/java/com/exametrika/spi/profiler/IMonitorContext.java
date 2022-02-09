/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;


import java.util.Map;

import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.tasks.ITaskQueue;
import com.exametrika.common.time.ITimeService;
import com.exametrika.spi.aggregator.common.meters.IMeasurementContext;


/**
 * The {@link IMonitorContext} represents a runtime context for monitors.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IMonitorContext extends IMeasurementContext {
    /**
     * Returns agent start arguments.
     *
     * @return agent start arguments
     */
    Map<String, String> getAgentArgs();

    /**
     * Returns profiler configuration.
     *
     * @return profiler configuration
     */
    ProfilerConfiguration getConfiguration();

    /**
     * Returns time service.
     *
     * @return time service
     */
    ITimeService getTimeService();

    /**
     * Returns context for instance fields.
     *
     * @return context for instance fields or null if context is not set
     */
    JsonObject getInstanceContext();

    /**
     * Sets context for instance fields.
     *
     * @param value context for instance fields or null if context is not set
     */
    void setInstanceContext(JsonObject value);

    /**
     * Returns task queue used for asynchronous measurements of long running monitors in thread pool.
     *
     * @return task queue used for asynchronous measurements of long running monitors in thread pool
     */
    ITaskQueue<Runnable> getTaskQueue();
}
