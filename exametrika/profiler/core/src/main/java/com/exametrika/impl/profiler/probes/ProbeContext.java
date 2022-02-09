/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.util.Map;

import com.exametrika.api.instrument.IClassTransformer;
import com.exametrika.api.instrument.IInstrumentationService;
import com.exametrika.api.instrument.IJoinPointProvider;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.aggregator.common.meters.AggregatingMeasurementHandler;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.impl.profiler.strategies.MeasurementStrategyManager;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;
import com.exametrika.spi.profiler.IMeasurementStrategy;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.ITimeSource;
import com.exametrika.spi.profiler.ITransactionInfo;


/**
 * The {@link ProbeContext} is a runtime context for probes.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ProbeContext implements IProbeContext {
    private final ThreadLocalAccessor threadLocalAccessor;
    private final IInstrumentationService instrumentationService;
    private final IJoinPointProvider joinPointProvider;
    private final IClassTransformer classTransformer;
    private final ITimeService timeService;
    private final IMeasurementHandler measurementHandler;
    private final AggregatingMeasurementHandler stackMeasurementHandler;
    private final ITimeSource timeSource;
    private volatile ProfilerConfiguration configuration;
    private final Map<String, String> agentArgs;
    private final MeasurementStrategyManager measurementStrategyManager;

    public ProbeContext(ThreadLocalAccessor threadLocalAccessor, IInstrumentationService instrumentationService,
                        IJoinPointProvider joinPointProvider, IClassTransformer classTransformer, ITimeService timeService,
                        IMeasurementHandler measurementHandler, ProfilerConfiguration configuration, Map<String, String> agentArgs,
                        MeasurementStrategyManager measurementStrategyManager) {
        Assert.notNull(threadLocalAccessor);
        Assert.notNull(joinPointProvider);
        Assert.notNull(timeService);
        Assert.notNull(measurementHandler);
        Assert.notNull(configuration);
        Assert.notNull(agentArgs);
        Assert.notNull(measurementStrategyManager);

        this.threadLocalAccessor = threadLocalAccessor;
        this.instrumentationService = instrumentationService;
        this.joinPointProvider = joinPointProvider;
        this.classTransformer = classTransformer;
        this.timeService = timeService;
        this.measurementHandler = measurementHandler;
        this.measurementStrategyManager = measurementStrategyManager;

        long preaggregationPeriod = configuration.getStackProbe() != null ? configuration.getStackProbe().getPreaggregationPeriod() : 5000;

        this.stackMeasurementHandler = new AggregatingMeasurementHandler(configuration.createAggregationSchema(),
                preaggregationPeriod, timeService, measurementHandler);
        this.configuration = configuration;
        this.agentArgs = Immutables.wrap(agentArgs);

        if (configuration.getTimeSource() == TimeSource.WALL_TIME)
            timeSource = new WallTimeSource();
        else if (configuration.getTimeSource() == TimeSource.THREAD_CPU_TIME)
            timeSource = new ThreadCpuTimeSource();
        else {
            timeSource = null;
            Assert.error();
        }
    }

    public ThreadLocalAccessor getThreadLocalAccessor() {
        return threadLocalAccessor;
    }

    public IInstrumentationService getInstrumentationService() {
        return instrumentationService;
    }

    public MeasurementStrategyManager getMeasurementStrategyManager() {
        return measurementStrategyManager;
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
    public int getSchemaVersion() {
        return configuration.getSchemaVersion();
    }

    @Override
    public ITimeSource getTimeSource() {
        return timeSource;
    }

    @Override
    public IJoinPointProvider getJoinPointProvider() {
        return joinPointProvider;
    }

    @Override
    public IClassTransformer getClassTransformer() {
        return classTransformer;
    }

    @Override
    public boolean isProbe(String className) {
        return threadLocalAccessor.getScopeContext().isProbe(className);
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
    public IMeasurementHandler getStackMeasurementHandler() {
        return stackMeasurementHandler;
    }

    @Override
    public IMeasurementStrategy findMeasurementStrategy(String name) {
        return measurementStrategyManager.findMeasurementStrategy(name);
    }

    @Override
    public ProfilerConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public ITransactionInfo getCurrentTransaction() {
        Container container = threadLocalAccessor.get();
        if (container == null)
            return null;
        StackProbeCollector top = (StackProbeCollector) container.top;
        if (top == null)
            return null;

        return top.getRoot().getTransaction();
    }

    @Override
    public JsonObject getInstanceContext() {
        Container container = threadLocalAccessor.get();
        if (container == null)
            return null;

        return container.contextProvider.getContext();
    }

    @Override
    public void setInstanceContext(JsonObject value) {
        Container container = threadLocalAccessor.get();
        if (container == null)
            return;

        container.contextProvider.setContext(value);
    }

    @Override
    public IScope getScope(String name, String type) {
        Container container = threadLocalAccessor.get();
        if (container == null)
            return null;

        return container.scopes.get(name, type);
    }

    public void onTimer() {
        stackMeasurementHandler.onTimer();
    }
}
