/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;


import java.util.Map;

import com.exametrika.api.instrument.IClassTransformer;
import com.exametrika.api.instrument.IJoinPointProvider;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.time.ITimeService;
import com.exametrika.spi.aggregator.common.meters.IMeasurementContext;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;


/**
 * The {@link IProbeContext} represents a runtime context for probes.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IProbeContext extends IMeasurementContext {
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
     * Returns profiler time source.
     *
     * @return profiler time source
     */
    ITimeSource getTimeSource();

    /**
     * Returns scope for the current thread.
     *
     * @param name scope name
     * @param type scope type
     * @return scope
     */
    IScope getScope(String name, String type);

    /**
     * Returns join point provider.
     *
     * @return join point provider
     */
    IJoinPointProvider getJoinPointProvider();

    /**
     * Returns class transformer.
     *
     * @return class transformer or null if class transformer is not available
     */
    IClassTransformer getClassTransformer();

    /**
     * Returns true if specified class name is probe static interceptor.
     *
     * @param className class name
     * @return true if specified class name is probe static interceptor
     */
    boolean isProbe(String className);

    /**
     * Returns transaction of current thread.
     *
     * @return transaction of current thread or null if transaction is not active in current thread
     */
    ITransactionInfo getCurrentTransaction();

    /**
     * Returns preaggregating stack measurement handler.
     *
     * @return preaggregating stack measurement handler
     */
    IMeasurementHandler getStackMeasurementHandler();

    /**
     * Finds measurement strategy by name.
     *
     * @param name measurement startegy name
     * @return measurement strategy or null if measurement strategy is not found
     */
    IMeasurementStrategy findMeasurementStrategy(String name);
}
