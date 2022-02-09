/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.config;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ProfilerRecorderConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.config.IConfigurationFactory;
import com.exametrika.common.config.IContextFactory;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Enums;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;


/**
 * The {@link ProfilerLoadContext} is a helper class that is used to load {@link ProfilerConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ProfilerLoadContext implements IProfilerLoadContext, IContextFactory, IConfigurationFactory {
    private String nodeName = "node";
    private Set<ScopeConfiguration> permanentScopes = new LinkedHashSet<ScopeConfiguration>();
    private Set<MonitorConfiguration> monitors = new LinkedHashSet<MonitorConfiguration>();
    private Set<ProbeConfiguration> probes = new LinkedHashSet<ProbeConfiguration>();
    private Set<MeasurementStrategyConfiguration> measurementStrategies = new LinkedHashSet<MeasurementStrategyConfiguration>();
    private int schemaVersion;
    private int monitorThreadPoolSize = Runtime.getRuntime().availableProcessors() * 4;
    private long monitorTimerPeriod = 100;
    private long fullExtractionPeriod = 300000;
    private long maxScopeIdlePeriod = 300000;
    private File workPath = new File(System.getProperty("com.exametrika.workPath"), "/profiler");
    private int maxInstrumentedMethodsCount = 100000;
    private Set<DumpType> dump = Enums.noneOf(DumpType.class);
    private long dumpPeriod = 60000;
    private TimeSource timeSource = TimeSource.WALL_TIME;
    private JsonObject nodeProperties = JsonUtils.EMPTY_OBJECT;
    private ProfilerRecorderConfiguration recorder;

    @Override
    public void addMonitor(MonitorConfiguration monitor) {
        monitors.add(monitor);
    }

    @Override
    public void addProbe(ProbeConfiguration probe) {
        probes.add(probe);
    }

    @Override
    public void addPermanentScope(ScopeConfiguration scope) {
        permanentScopes.add(scope);
    }

    @Override
    public void addMeasurementStrategy(MeasurementStrategyConfiguration measurementStrategy) {
        measurementStrategies.add(measurementStrategy);
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public void setMonitorThreadPoolSize(int monitorThreadPoolSize) {
        this.monitorThreadPoolSize = monitorThreadPoolSize;
    }

    public void setMonitorTimerPeriod(long monitorTimerPeriod) {
        this.monitorTimerPeriod = monitorTimerPeriod;
    }

    public void setFullExtractionPeriod(long fullExtractionPeriod) {
        this.fullExtractionPeriod = fullExtractionPeriod;
    }

    public void setMaxScopeIdlePeriod(long maxScopeIdlePeriod) {
        this.maxScopeIdlePeriod = maxScopeIdlePeriod;
    }

    public void setWorkPath(File workPath) {
        Assert.notNull(workPath);

        this.workPath = new File(workPath, "profiler");
    }

    public void setMaxInstrumentedMethodsCount(int value) {
        this.maxInstrumentedMethodsCount = value;
    }

    public void setTimeSource(TimeSource timeSource) {
        Assert.notNull(timeSource);

        this.timeSource = timeSource;
    }

    public void setDump(Set<DumpType> value) {
        Assert.notNull(value);

        dump = value;
    }

    public void setDumpPeriod(long period) {
        this.dumpPeriod = period;
    }

    public void setNodeProperties(JsonObject nodeProperties) {
        Assert.notNull(nodeProperties);

        this.nodeProperties = nodeProperties;
    }

    public void setRecorder(ProfilerRecorderConfiguration recorder) {
        this.recorder = recorder;
    }

    @Override
    public Object createConfiguration(ILoadContext context) {
        return new ProfilerConfiguration(nodeName, timeSource, measurementStrategies, permanentScopes, monitors, probes,
                schemaVersion, monitorThreadPoolSize, monitorTimerPeriod, fullExtractionPeriod, maxScopeIdlePeriod, workPath,
                maxInstrumentedMethodsCount, dump, dumpPeriod, nodeProperties, recorder);
    }

    @Override
    public IConfigurationFactory createContext() {
        return new ProfilerLoadContext();
    }
}
