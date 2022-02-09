/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.common.values.AggregationSchema;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;
import com.exametrika.spi.profiler.config.EntryPointProbeConfiguration;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;


/**
 * The {@link ProfilerConfiguration} is a profiler configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ProfilerConfiguration extends Configuration {
    public static final String SCHEMA = "com.exametrika.profiler-1.0";
    public static final int MAX_SCOPE_TYPE_COUNT = 16;
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String nodeName;
    private final TimeSource timeSource;
    private final Set<MeasurementStrategyConfiguration> measurementStrategies;
    private final Set<ScopeConfiguration> permanentScopes;
    private final Set<MonitorConfiguration> monitors;
    private final Set<ProbeConfiguration> probes;
    private final StackProbeConfiguration stackProbe;
    private final int schemaVersion;
    private final int monitorThreadPoolSize;
    private final long monitorTimerPeriod;
    private final long fullExtractionPeriod;
    private final long maxScopeIdlePeriod;
    private final File workPath;
    private final int maxInstrumentedMethodsCount;
    private final Set<DumpType> dump;
    private final long dumpPeriod;
    private final JsonObject nodeProperties;
    private final ProfilerRecorderConfiguration recorder;

    public ProfilerConfiguration(String nodeName, TimeSource timeSource, Set<? extends MeasurementStrategyConfiguration> measurementStrategies,
                                 Set<ScopeConfiguration> permanentScopes, Set<? extends MonitorConfiguration> monitors,
                                 Set<? extends ProbeConfiguration> probes, int schemaVersion,
                                 int monitorThreadPoolSize, long monitorTimerPeriod, long fullExtractionPeriod, long maxScopeIdlePeriod,
                                 File workPath, int maxInstrumentedMethodsCount, Set<DumpType> dump, long dumpPeriod, JsonObject nodeProperties,
                                 ProfilerRecorderConfiguration recorder) {
        Assert.notNull(nodeName);
        Assert.isTrue(!nodeName.isEmpty());
        Assert.notNull(timeSource);
        Assert.notNull(measurementStrategies);
        Assert.notNull(permanentScopes);
        Assert.notNull(monitors);
        Assert.notNull(probes);
        Assert.isTrue(monitorThreadPoolSize > 0);
        Assert.isTrue(monitorTimerPeriod > 0);
        Assert.isTrue(fullExtractionPeriod > 0);
        Assert.notNull(workPath);
        Assert.notNull(dump);
        Assert.notNull(nodeProperties);

        Map<String, MeasurementStrategyConfiguration> strategiesMap = new LinkedHashMap<String, MeasurementStrategyConfiguration>();
        for (MeasurementStrategyConfiguration strategy : measurementStrategies) {
            if (strategiesMap.put(strategy.getName(), strategy) != null)
                throw new InvalidArgumentException(messages.strategyAlreadyExists(strategy.getName()));
        }

        Set<String> scopeTypes = new HashSet<String>();
        for (ScopeConfiguration scope : permanentScopes) {
            if (!scopeTypes.add(scope.getType()))
                throw new InvalidArgumentException(messages.permanentScopeAlreadyExists(scope.getType()));
        }

        StackProbeConfiguration stackProbe = null;
        for (ProbeConfiguration probe : probes) {
            scopeTypes.add(probe.getScopeType());

            if (probe instanceof StackProbeConfiguration) {
                Assert.isNull(stackProbe);
                stackProbe = (StackProbeConfiguration) probe;
            }
        }

        this.stackProbe = stackProbe;

        if (scopeTypes.size() > MAX_SCOPE_TYPE_COUNT)
            throw new InvalidArgumentException(messages.tooManyScopeTypes(scopeTypes.size(), MAX_SCOPE_TYPE_COUNT));

        Map<String, MonitorConfiguration> monitorsMap = new LinkedHashMap<String, MonitorConfiguration>();
        for (MonitorConfiguration monitor : monitors) {
            if (monitorsMap.put(monitor.getName(), monitor) != null)
                throw new InvalidArgumentException(messages.monitorAlreadyExists(monitor.getName()));
            if (fullExtractionPeriod / monitor.getPeriod() < 2 || fullExtractionPeriod % monitor.getPeriod() != 0)
                throw new InvalidArgumentException(messages.periodNotValid(monitor.getName()));

            checkMeasurementStrategy(strategiesMap, monitor.getName(), monitor.getMeasurementStrategy());
        }

        Map<String, ProbeConfiguration> probesMap = new LinkedHashMap<String, ProbeConfiguration>();
        for (ProbeConfiguration probe : probes) {
            if (probesMap.put(probe.getClass().getName(), probe) != null)
                throw new InvalidArgumentException(messages.probeAlreadyExists(probe.getName()));
            if (!(probe instanceof EntryPointProbeConfiguration || probe instanceof ExitPointProbeConfiguration) &&
                    (fullExtractionPeriod / probe.getExtractionPeriod() < 2 || fullExtractionPeriod % probe.getExtractionPeriod() != 0))
                throw new InvalidArgumentException(messages.periodNotValid(probe.getName()));

            checkMeasurementStrategy(strategiesMap, probe.getName(), probe.getMeasurementStrategy());
            if (probe instanceof StackProbeConfiguration)
                checkMeasurementStrategy(strategiesMap, probe.getName(), ((StackProbeConfiguration) probe).getStackMeasurementStrategy());
            if (probe instanceof EntryPointProbeConfiguration)
                checkMeasurementStrategy(strategiesMap, probe.getName(), ((EntryPointProbeConfiguration) probe).getStackMeasurementStrategy());
        }

        this.nodeName = nodeName;
        this.timeSource = timeSource;
        this.measurementStrategies = Immutables.wrap(measurementStrategies);
        this.permanentScopes = Immutables.wrap(permanentScopes);
        this.monitors = Immutables.wrap(monitors);
        this.probes = Immutables.wrap(probes);
        this.schemaVersion = schemaVersion;
        this.monitorThreadPoolSize = monitorThreadPoolSize;
        this.monitorTimerPeriod = monitorTimerPeriod;
        this.fullExtractionPeriod = fullExtractionPeriod;
        this.maxScopeIdlePeriod = maxScopeIdlePeriod;
        this.workPath = workPath;
        this.maxInstrumentedMethodsCount = maxInstrumentedMethodsCount;
        this.dump = Immutables.wrap(dump);
        this.dumpPeriod = dumpPeriod;
        this.nodeProperties = nodeProperties;
        this.recorder = recorder;
    }

    public String getNodeName() {
        return nodeName;
    }

    public TimeSource getTimeSource() {
        return timeSource;
    }

    public Set<MeasurementStrategyConfiguration> getMeasurementStrategies() {
        return measurementStrategies;
    }

    public Set<ScopeConfiguration> getPermanentScopes() {
        return permanentScopes;
    }

    public Set<MonitorConfiguration> getMonitors() {
        return monitors;
    }

    public Set<ProbeConfiguration> getProbes() {
        return probes;
    }

    public StackProbeConfiguration getStackProbe() {
        return stackProbe;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public int getMonitorThreadPoolSize() {
        return monitorThreadPoolSize;
    }

    public long getMonitorTimerPeriod() {
        return monitorTimerPeriod;
    }

    public long getFullExtractionPeriod() {
        return fullExtractionPeriod;
    }

    public long getMaxScopeIdlePeriod() {
        return maxScopeIdlePeriod;
    }

    public File getWorkPath() {
        return workPath;
    }

    public int getMaxInstrumentedMethodsCount() {
        return maxInstrumentedMethodsCount;
    }

    public Set<DumpType> getDump() {
        return dump;
    }

    public long getDumpPeriod() {
        return dumpPeriod;
    }

    public JsonObject getNodeProperties() {
        return nodeProperties;
    }

    public ProfilerRecorderConfiguration getRecorder() {
        return recorder;
    }

    public IAggregationSchema createAggregationSchema() {
        Set<ComponentValueSchemaConfiguration> components = new LinkedHashSet<ComponentValueSchemaConfiguration>();

        for (MonitorConfiguration monitor : monitors)
            monitor.buildComponentSchemas(components);

        List<MetricValueSchemaConfiguration> stackMetrics = null;
        if (stackProbe != null)
            stackMetrics = stackProbe.getStackMetrics();

        for (ProbeConfiguration probe : probes) {
            if (probe instanceof EntryPointProbeConfiguration) {
                EntryPointProbeConfiguration entryPointProbe = (EntryPointProbeConfiguration) probe;

                for (String prefix : new String[]{"primary.", "secondary."}) {
                    ComponentValueSchemaBuilder builder = ValueSchemas.component(prefix + entryPointProbe.getComponentType());
                    for (MetricValueSchemaConfiguration metric : stackMetrics)
                        builder.metric(metric);

                    entryPointProbe.buildComponentSchemas(builder, components);

                    components.add(builder.toConfiguration());
                }
            } else if (probe instanceof ExitPointProbeConfiguration) {
                ExitPointProbeConfiguration exitPointProbe = (ExitPointProbeConfiguration) probe;

                ComponentValueSchemaBuilder builder = ValueSchemas.component(exitPointProbe.getComponentType());
                for (MetricValueSchemaConfiguration metric : stackMetrics)
                    builder.metric(metric);

                exitPointProbe.buildComponentSchemas(builder, components);

                components.add(builder.toConfiguration());
            } else
                probe.buildComponentSchemas(components);
        }

        return new AggregationSchema(components, schemaVersion);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ProfilerConfiguration))
            return false;

        ProfilerConfiguration configuration = (ProfilerConfiguration) o;
        return nodeName.equals(configuration.nodeName) && timeSource == configuration.timeSource &&
                measurementStrategies.equals(configuration.measurementStrategies) &&
                permanentScopes.equals(configuration.permanentScopes) &&
                monitors.equals(configuration.monitors) && probes.equals(configuration.probes) &&
                schemaVersion == configuration.schemaVersion &&
                monitorThreadPoolSize == configuration.monitorThreadPoolSize &&
                monitorTimerPeriod == configuration.monitorTimerPeriod &&
                fullExtractionPeriod == configuration.fullExtractionPeriod &&
                maxScopeIdlePeriod == configuration.maxScopeIdlePeriod &&
                workPath.equals(configuration.workPath) &&
                maxInstrumentedMethodsCount == configuration.maxInstrumentedMethodsCount &&
                dump.equals(configuration.dump) && dumpPeriod == configuration.dumpPeriod &&
                nodeProperties.equals(configuration.nodeProperties) && Objects.equals(recorder, configuration.recorder);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nodeName, timeSource, measurementStrategies, permanentScopes, monitors, probes,
                schemaVersion, monitorThreadPoolSize, monitorTimerPeriod, fullExtractionPeriod, maxScopeIdlePeriod, workPath,
                maxInstrumentedMethodsCount, dump, dumpPeriod, nodeProperties, recorder);
    }

    @Override
    public String toString() {
        return nodeName;
    }

    private static void checkMeasurementStrategy(Map<String, MeasurementStrategyConfiguration> strategies, String name,
                                                 String measurementStrategy) {
        if (measurementStrategy != null) {
            MeasurementStrategyConfiguration existing = strategies.get(measurementStrategy);
            if (existing == null)
                throw new InvalidArgumentException(messages.strategyNotFound(name, measurementStrategy));
        }
    }

    private interface IMessages {
        @DefaultMessage("Monitor ''{0}'' already exists.")
        ILocalizedMessage monitorAlreadyExists(String monitor);

        @DefaultMessage("Probe with the same class as probe ''{0}'' already exists.")
        ILocalizedMessage probeAlreadyExists(String probe);

        @DefaultMessage("Too many scope types: {0}. Maximum allowed: {1}.")
        ILocalizedMessage tooManyScopeTypes(int scopeCount, int maxScopeCount);

        @DefaultMessage("Permanent scope with type ''{0}'' already exists.")
        ILocalizedMessage permanentScopeAlreadyExists(String scope);

        @DefaultMessage("Measurement strategy ''{1}''  of ''{0}'' is not found.")
        ILocalizedMessage strategyNotFound(String name, String strategy);

        @DefaultMessage("Measurement strategy ''{0}'' already exists.")
        ILocalizedMessage strategyAlreadyExists(String strategy);

        @DefaultMessage("Full extraction period must be integral multiple of ''{0}'' period.")
        ILocalizedMessage periodNotValid(String name);
    }
}
