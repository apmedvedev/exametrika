/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.aggregator.common.meters.config.StandardFieldConfiguration;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.InterceptPointcut.Kind;
import com.exametrika.api.instrument.config.MemberFilter;
import com.exametrika.api.instrument.config.MemberNameFilter;
import com.exametrika.api.instrument.config.NewArrayPointcut;
import com.exametrika.api.instrument.config.NewObjectPointcut;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.api.profiler.config.AllocationProbeConfiguration;
import com.exametrika.api.profiler.config.AppStackCounterConfiguration;
import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.api.profiler.config.CheckPointMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.CompositeMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.CompositeMeasurementStrategyConfiguration.Type;
import com.exametrika.api.profiler.config.CompositeRequestMappingStrategyConfiguration;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.ExceptionProbeConfiguration;
import com.exametrika.api.profiler.config.ExternalMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.HighCpuMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.HighMemoryMeasurementStrategyConfiguration;
import com.exametrika.api.profiler.config.HotspotRequestMappingStrategyConfiguration;
import com.exametrika.api.profiler.config.MeasurementsGeneratorMonitorConfiguration;
import com.exametrika.api.profiler.config.MethodEntryPointProbeConfiguration;
import com.exametrika.api.profiler.config.MethodExitPointProbeConfiguration;
import com.exametrika.api.profiler.config.MonitorSetConfiguration;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ProfilerRecorderConfiguration;
import com.exametrika.api.profiler.config.ReplayMonitorConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.SimpleRequestMappingStrategyConfiguration;
import com.exametrika.api.profiler.config.StackInterceptPointcut;
import com.exametrika.api.profiler.config.StackProbeConfiguration;
import com.exametrika.api.profiler.config.StackProbeConfiguration.CombineType;
import com.exametrika.api.profiler.config.ThreadEntryPointProbeConfiguration;
import com.exametrika.api.profiler.config.ThreadExitPointInterceptPointcut;
import com.exametrika.api.profiler.config.ThreadExitPointProbeConfiguration;
import com.exametrika.api.profiler.config.ThresholdRequestMappingStrategyConfiguration;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.config.AbstractElementLoader;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.profiler.boot.AgentStackProbeInterceptor;
import com.exametrika.impl.profiler.boot.AgentlessStackProbeInterceptor;
import com.exametrika.impl.profiler.boot.AllocationProbeInterceptor;
import com.exametrika.impl.profiler.boot.CheckPointMeasurementStrategyInterceptor;
import com.exametrika.impl.profiler.boot.ExceptionProbeInterceptor;
import com.exametrika.impl.profiler.boot.MainInterceptor;
import com.exametrika.impl.profiler.boot.MethodEntryPointProbeInterceptor;
import com.exametrika.impl.profiler.boot.MethodExitPointProbeInterceptor;
import com.exametrika.impl.profiler.boot.ThreadExitPointProbeInterceptor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.FieldConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.GaugeConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.instrument.config.IInstrumentationLoadContext;
import com.exametrika.spi.instrument.config.StaticInterceptorConfiguration;
import com.exametrika.spi.profiler.config.EntryPointProbeConfiguration.PrimaryType;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;
import com.exametrika.spi.profiler.config.RequestGroupingStrategyConfiguration;
import com.exametrika.spi.profiler.config.RequestMappingStrategyConfiguration;
import com.exametrika.spi.profiler.config.StackCounterConfiguration;


/**
 * The {@link ProfilerConfigurationLoader} is a configuration loader for profiler configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ProfilerConfigurationLoader extends AbstractElementLoader implements IExtensionLoader {
    @Override
    public void loadElement(JsonObject element, ILoadContext context) {
        ProfilerLoadContext loadContext = context.get(ProfilerConfiguration.SCHEMA);

        JsonObject elements = element.get("permanentScopes", null);
        if (elements != null) {
            for (Map.Entry<String, Object> entry : elements)
                loadContext.addPermanentScope(loadScope(entry.getKey(), (JsonObject) entry.getValue()));
        }

        elements = element.get("monitors", null);
        if (elements != null) {
            for (Map.Entry<String, Object> entry : elements) {
                MonitorConfiguration monitor = loadMonitor(entry.getKey(), (JsonObject) entry.getValue(), context);
                loadContext.addMonitor(monitor);
            }
        }

        elements = element.get("probes", null);
        if (elements != null) {
            for (Map.Entry<String, Object> entry : elements) {
                ProbeConfiguration probe = loadProbe(entry.getKey(), (JsonObject) entry.getValue(), context);
                loadContext.addProbe(probe);
            }
        }

        elements = element.get("measurementStrategies", null);
        if (elements != null) {
            for (Map.Entry<String, Object> entry : elements) {
                MeasurementStrategyConfiguration measurementStrategy = loadMeasurementStrategy(entry.getKey(),
                        (JsonObject) entry.getValue(), context);
                loadContext.addMeasurementStrategy(measurementStrategy);
            }
        }

        loadContext.setSchemaVersion(((Long) element.get("schemaVersion")).intValue());
        loadContext.setMonitorThreadPoolSize(((Long) element.get("monitorThreadPoolSize",
                Runtime.getRuntime().availableProcessors() * 2l)).intValue());
        loadContext.setMonitorTimerPeriod((Long) element.get("monitorTimerPeriod"));
        loadContext.setFullExtractionPeriod((Long) element.get("fullExtractionPeriod"));
        loadContext.setMaxScopeIdlePeriod((Long) element.get("maxScopeIdlePeriod"));
        loadContext.setTimeSource(loadTimeSource((String) element.get("timeSource")));
        loadContext.setDump(loadDump((JsonArray) element.get("dump")));
        loadContext.setDumpPeriod((Long) element.get("dumpPeriod"));
        loadContext.setRecorder(loadRecorder((JsonObject) element.get("recorder", null)));

        int maxInstrumentedMethodsCount = ((Long) element.get("maxInstrumentedMethodsCount")).intValue();
        loadContext.setMaxInstrumentedMethodsCount(maxInstrumentedMethodsCount);
        IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
        instrumentationContext.setMaxJoinPointCount(maxInstrumentedMethodsCount);
    }

    private ProfilerRecorderConfiguration loadRecorder(JsonObject element) {
        if (element == null)
            return null;

        return new ProfilerRecorderConfiguration((String) element.get("fileName"),
                (Long) element.get("delayPeriod"), (Long) element.get("recordPeriod"));
    }

    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("SimpleRequestMappingStrategy")) {
            String nameExpression = element.get("nameExpression");
            String metadataExpression = element.get("metadataExpression");
            String parametersExpression = element.get("parametersExpression");
            String requestFilter = element.get("requestFilter", null);
            return new SimpleRequestMappingStrategyConfiguration(nameExpression, metadataExpression,
                    parametersExpression, requestFilter);
        } else if (type.equals("HotspotRequestMappingStrategy")) {
            String nameExpression = element.get("nameExpression");
            String metadataExpression = element.get("metadataExpression");
            String parametersExpression = element.get("parametersExpression");
            String requestFilter = element.get("requestFilter", null);
            String beginValueExpression = element.get("beginValueExpression", null);
            String endValueExpression = element.get("endValueExpression", null);
            long estimationPeriod = element.get("estimationPeriod");
            long measurementPeriod = element.get("measurementPeriod");
            long minHotspotCount = element.get("minHotspotCount");
            long maxHotspotCount = element.get("maxHotspotCount");
            long hotspotStep = element.get("hotspotStep");
            double hotspotCoverage = element.get("hotspotCoverage");
            long maxRequestCount = element.get("maxRequestCount");
            RequestGroupingStrategyConfiguration groupingStrategy = load(null, null, (JsonObject) element.get("groupingStrategy"), context);
            boolean perThreadStatistics = element.get("perThreadStatistics");
            return new HotspotRequestMappingStrategyConfiguration(nameExpression, metadataExpression,
                    parametersExpression, requestFilter, beginValueExpression, endValueExpression, estimationPeriod,
                    measurementPeriod, (int) minHotspotCount, (int) maxHotspotCount, (int) hotspotStep, hotspotCoverage,
                    (int) maxRequestCount, groupingStrategy, perThreadStatistics);
        } else if (type.equals("ThresholdRequestMappingStrategy")) {
            String nameExpression = element.get("nameExpression");
            String metadataExpression = element.get("metadataExpression");
            String parametersExpression = element.get("parametersExpression");
            String requestFilter = element.get("requestFilter", null);
            String beginValueExpression = element.get("beginValueExpression", null);
            String endValueExpression = element.get("endValueExpression", null);
            long threshold = element.get("threshold");
            long estimationPeriod = element.get("estimationPeriod");
            long measurementPeriod = element.get("measurementPeriod");
            long maxRequestCount = element.get("maxRequestCount");
            double requestPercentage = element.get("requestPercentage");
            return new ThresholdRequestMappingStrategyConfiguration(nameExpression, metadataExpression,
                    parametersExpression, requestFilter, beginValueExpression, endValueExpression,
                    threshold, estimationPeriod, measurementPeriod, (int) maxRequestCount, requestPercentage);
        } else if (type.equals("CompositeRequestMappingStrategy")) {
            JsonArray array = element.get("strategies");
            List<RequestMappingStrategyConfiguration> strategies = new ArrayList<RequestMappingStrategyConfiguration>();
            for (Object strategy : array)
                strategies.add((RequestMappingStrategyConfiguration) load(null, null, (JsonObject) strategy, context));
            return new CompositeRequestMappingStrategyConfiguration(strategies);
        } else
            throw new InvalidConfigurationException();
    }

    private ScopeConfiguration loadScope(String name, JsonObject element) {
        String id = element.get("id", null);
        String type = element.get("type");
        String threadFilter = element.get("threadFilter", null);
        return new ScopeConfiguration(name, id, type, threadFilter);
    }

    private MonitorConfiguration loadMonitor(String name, JsonObject element, ILoadContext context) {
        String type = getType(element);

        if (type.equals("MonitorSet")) {
            long period = element.get("period");
            String measurementStrategy = element.get("measurementStrategy", null);

            Set<MonitorConfiguration> monitors = new LinkedHashSet<MonitorConfiguration>();
            JsonObject elements = element.get("monitors", null);
            if (elements != null) {
                for (Map.Entry<String, Object> entry : elements) {
                    MonitorConfiguration monitor = loadMonitor(entry.getKey(), (JsonObject) entry.getValue(), context);
                    monitors.add(monitor);
                }
            }

            return new MonitorSetConfiguration(name, period, measurementStrategy, monitors);
        } else if (type.equals("MeasurementsGeneratorMonitor")) {
            String scope = element.get("scope", null);
            long period = element.get("period");
            String measurementStrategy = element.get("measurementStrategy", null);

            long nodesCount = element.get("nodesCount");
            long primaryEntryPointNodesCount = element.get("primaryEntryPointNodesCount");
            long transactionsPerNodeCount = element.get("transactionsPerNodeCount");
            long transactionSegmentsDepth = element.get("transactionSegmentsDepth");
            long logRecordsCount = element.get("logRecordsCount");
            long stackDepth = element.get("stackDepth");
            long leafStackEntriesCount = element.get("leafStackEntriesCount");
            long maxEndExitPointsCount = element.get("maxEndExitPointsCount");
            long maxIntermediateExitPointsCount = element.get("maxIntermediateExitPointsCount");
            long exitPointsPerEntryCount = element.get("exitPointsPerEntryCount");

            String combineTypeStr = element.get("combineType");
            CombineType combineType;
            if (combineTypeStr.equals("stack"))
                combineType = CombineType.STACK;
            else if (combineTypeStr.equals("transaction"))
                combineType = CombineType.TRANSACTION;
            else if (combineTypeStr.equals("node"))
                combineType = CombineType.NODE;
            else if (combineTypeStr.equals("all"))
                combineType = CombineType.ALL;
            else
                combineType = Assert.error();

            String measurementProfile = element.get("measurementProfile");

            return new MeasurementsGeneratorMonitorConfiguration(name, scope, measurementStrategy, period, (int) nodesCount, (int) primaryEntryPointNodesCount,
                    (int) transactionsPerNodeCount, (int) transactionSegmentsDepth, (int) logRecordsCount, (int) stackDepth, (int) leafStackEntriesCount,
                    (int) maxEndExitPointsCount, (int) maxIntermediateExitPointsCount, (int) exitPointsPerEntryCount,
                    combineType, measurementProfile);
        } else if (type.equals("ReplayMonitor")) {
            String scope = element.get("scope", null);
            long period = element.get("period");
            String measurementStrategy = element.get("measurementStrategy", null);

            long nodesCount = element.get("nodesCount");
            String fileName = element.get("fileName");
            long startPeriod = element.get("startPeriod");
            return new ReplayMonitorConfiguration(name, scope, measurementStrategy, period, (int) nodesCount, fileName, startPeriod);
        } else
            return load(name, type, element, context);
    }

    private ProbeConfiguration loadProbe(String name, JsonObject element, ILoadContext context) {
        String type = getType(element);

        if (type.equals("StackProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            String stackMeasurementStrategy = element.get("stackMeasurementStrategy", null);
            long extractionPeriod = element.get("extractionPeriod");
            long warmupDelay = element.get("warmupDelay");
            long minEstimationPeriod = element.get("minEstimationPeriod");
            long maxEstimationPeriod = element.get("maxEstimationPeriod");
            long minHotspotCount = element.get("minHotspotCount");
            long maxHotspotCount = element.get("maxHotspotCount");
            long hotspotStep = element.get("hotspotStep");
            double hotspotCoverage = element.get("hotspotCoverage");
            double tolerableOverhead = element.get("tolerableOverhead");
            long ultraFastMethodThreshold = element.get("ultraFastMethodThreshold");
            long idleRetentionCount = element.get("idleRetentionCount");
            long extractionDelayCount = element.get("extractionDelayCount");
            long preaggregationPeriod = element.get("preaggregationPeriod");

            String combineTypeStr = element.get("combineType");
            CombineType combineType;
            if (combineTypeStr.equals("stack"))
                combineType = CombineType.STACK;
            else if (combineTypeStr.equals("transaction"))
                combineType = CombineType.TRANSACTION;
            else if (combineTypeStr.equals("node"))
                combineType = CombineType.NODE;
            else if (combineTypeStr.equals("all"))
                combineType = CombineType.ALL;
            else
                combineType = Assert.error();

            GaugeConfiguration concurrencyLevel = load(null, "Gauge", (JsonObject) element.get("concurrencyLevel"), context);

            QualifiedMethodFilter methodFilter = load(null, "QualifiedMethodFilter", (JsonObject) element.get("intercepted", null), context);
            List<FieldConfiguration> fields = loadFields((JsonObject) element.get("fields", null), context);
            List<StackCounterConfiguration> stackCounters = loadStackCounters((JsonArray) element.get("stackCounters", null), context);
            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            StackInterceptPointcut stackPointcut = new StackInterceptPointcut(name, methodFilter, ThreadLocalAccessor.underAgent ?
                    AgentStackProbeInterceptor.class : AgentlessStackProbeInterceptor.class);
            instrumentationContext.addPointcut(stackPointcut);

            methodFilter = new QualifiedMethodFilter(new ClassFilter("*"), new MemberFilter("main(String[]):void"));
            InterceptPointcut mainPointcut = new InterceptPointcut("main", methodFilter, Enums.of(Kind.ENTER),
                    new StaticInterceptorConfiguration(MainInterceptor.class), false, false, 0);
            instrumentationContext.addPointcut(mainPointcut);

            return new StackProbeConfiguration(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay, fields,
                    stackCounters, concurrencyLevel, minEstimationPeriod, maxEstimationPeriod, (int) minHotspotCount,
                    (int) maxHotspotCount, (int) hotspotStep, hotspotCoverage, tolerableOverhead, ultraFastMethodThreshold,
                    (int) idleRetentionCount, (int) extractionDelayCount, preaggregationPeriod, combineType, stackMeasurementStrategy);
        } else if (type.equals("ExceptionProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long extractionPeriod = element.get("extractionPeriod");
            long warmupDelay = element.get("warmupDelay");
            LogConfiguration log = load(null, "Log", (JsonObject) element.get("log"), context);
            ClassFilter classFilter = load(null, "CompoundClassFilterExpression", element.get("intercepted", null), context);
            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            QualifiedMethodFilter methodFilter = new QualifiedMethodFilter(classFilter, new MemberFilter("<init>*"));
            InterceptPointcut pointcut = new InterceptPointcut(name, methodFilter, Enums.of(Kind.RETURN_EXIT),
                    new StaticInterceptorConfiguration(ExceptionProbeInterceptor.class), false, true, 0);
            instrumentationContext.addPointcut(pointcut);

            return new ExceptionProbeConfiguration(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay, log);
        } else if (type.equals("AllocationProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long extractionPeriod = element.get("extractionPeriod");
            long warmupDelay = element.get("warmupDelay");
            QualifiedMethodFilter methodFilter = load(null, "QualifiedMethodFilter", (JsonObject) element.get("intercepted", null), context);
            ClassNameFilter classNameFilter = load(null, "CompoundClassNameFilterExpression", element.get("allocated", null), context);

            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            Pointcut pointcut = new NewObjectPointcut(name + "1", methodFilter,
                    new StaticInterceptorConfiguration(AllocationProbeInterceptor.class), classNameFilter, true);
            instrumentationContext.addPointcut(pointcut);
            pointcut = new NewArrayPointcut(name + "2", methodFilter,
                    new StaticInterceptorConfiguration(AllocationProbeInterceptor.class), classNameFilter, true);
            instrumentationContext.addPointcut(pointcut);

            QualifiedMethodFilter constructorFilter = new QualifiedMethodFilter(new ClassFilter("java.lang.reflect.Constructor"),
                    new MemberFilter("newInstance(*"));
            QualifiedMethodFilter classLoaderFilter = new QualifiedMethodFilter(new ClassFilter("java.lang.ClassLoader"),
                    new MemberFilter(new MemberNameFilter(Arrays.asList(
                            "defineClass(String,byte[],int,int,ProtectionDomain)*",
                            "defineClass(String,ByteBuffer,ProtectionDomain)*"), null), null));
            methodFilter = new QualifiedMethodFilter(Arrays.asList(constructorFilter, classLoaderFilter), null);

            pointcut = new InterceptPointcut(name + "3", methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                    new StaticInterceptorConfiguration(AllocationProbeInterceptor.class), false, false, 0);
            instrumentationContext.addPointcut(pointcut);

            return new AllocationProbeConfiguration(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay);
        } else if (type.equals("ThreadExitPointProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long warmupDelay = element.get("warmupDelay");

            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            QualifiedMethodFilter executeFilter = new QualifiedMethodFilter(new ClassFilter("java.util.concurrent.ThreadPoolExecutor"),
                    new MemberFilter("execute(Runnable)*"));
            InterceptPointcut pointcut = new ThreadExitPointInterceptPointcut(name + "1", executeFilter, Enums.of(Kind.ENTER),
                    new StaticInterceptorConfiguration(ThreadExitPointProbeInterceptor.class), false, ExitPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(pointcut);

            QualifiedMethodFilter threadStartFilter = new QualifiedMethodFilter(new ClassFilter("java.lang.Thread"),
                    new MemberFilter("start()*"));
            QualifiedMethodFilter methodFilter = new QualifiedMethodFilter(Arrays.asList(executeFilter, threadStartFilter), null);
            pointcut = new InterceptPointcut(name + "2", methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                    new StaticInterceptorConfiguration(ThreadExitPointProbeInterceptor.class), true, false,
                    ExitPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(pointcut);

            return new ThreadExitPointProbeConfiguration(name, scopeType, measurementStrategy, warmupDelay);
        } else if (type.equals("ThreadEntryPointProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long warmupDelay = element.get("warmupDelay");
            long maxDuration = element.get("maxDuration");
            CounterConfiguration transactionTimeCounter = load(null, "Counter", (JsonObject) element.get("transactionTimeCounter"), context);
            LogConfiguration stalledRequestsLog = load(null, "Log", (JsonObject) element.get("stalledRequestsLog"), context);
            CounterConfiguration timeCounter = load(null, "Counter", (JsonObject) element.get("timeCounter"), context);
            CounterConfiguration receiveBytesCounter = load(null, "Counter", (JsonObject) element.get("receiveBytesCounter"), context);
            CounterConfiguration sendBytesCounter = load(null, "Counter", (JsonObject) element.get("sendBytesCounter"), context);
            LogConfiguration errorsLog = load(null, "Log", (JsonObject) element.get("errorsLog"), context);
            String allowPrimaryStr = element.get("allowPrimary");
            PrimaryType allowPrimary;
            if (allowPrimaryStr.equals("yes"))
                allowPrimary = PrimaryType.YES;
            else if (allowPrimaryStr.equals("no"))
                allowPrimary = PrimaryType.NO;
            else if (allowPrimaryStr.equals("always"))
                allowPrimary = PrimaryType.ALWAYS;
            else
                allowPrimary = Assert.error();

            boolean allowSecondary = element.get("allowSecondary");

            return new ThreadEntryPointProbeConfiguration(name, scopeType, measurementStrategy, warmupDelay,
                    maxDuration, transactionTimeCounter, stalledRequestsLog, allowPrimary, allowSecondary, timeCounter, receiveBytesCounter,
                    sendBytesCounter, errorsLog);
        } else if (type.equals("MethodEntryPointProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long warmupDelay = element.get("warmupDelay");
            long maxDuration = element.get("maxDuration");
            CounterConfiguration transactionTimeCounter = load(null, "Counter", (JsonObject) element.get("transactionTimeCounter"), context);
            LogConfiguration stalledRequestsLog = load(null, "Log", (JsonObject) element.get("stalledRequestsLog"), context);
            CounterConfiguration timeCounter = load(null, "Counter", (JsonObject) element.get("timeCounter"), context);
            CounterConfiguration receiveBytesCounter = load(null, "Counter", (JsonObject) element.get("receiveBytesCounter"), context);
            CounterConfiguration sendBytesCounter = load(null, "Counter", (JsonObject) element.get("sendBytesCounter"), context);
            LogConfiguration errorsLog = load(null, "Log", (JsonObject) element.get("errorsLog"), context);

            QualifiedMethodFilter methodFilter = load(null, "QualifiedMethodFilter", (JsonObject) element.get("intercepted", null), context);
            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            InterceptPointcut stackPointcut = new InterceptPointcut(name, methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                    new StaticInterceptorConfiguration(MethodEntryPointProbeInterceptor.class), false, false,
                    ExitPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(stackPointcut);

            return new MethodEntryPointProbeConfiguration(name, scopeType, measurementStrategy, warmupDelay,
                    maxDuration, transactionTimeCounter, stalledRequestsLog, timeCounter, receiveBytesCounter, sendBytesCounter, errorsLog);
        } else if (type.equals("MethodExitPointProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long warmupDelay = element.get("warmupDelay");

            QualifiedMethodFilter methodFilter = load(null, "QualifiedMethodFilter", (JsonObject) element.get("intercepted", null), context);
            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            InterceptPointcut stackPointcut = new InterceptPointcut(name, methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                    new StaticInterceptorConfiguration(MethodExitPointProbeInterceptor.class), false, false,
                    ExitPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(stackPointcut);

            return new MethodExitPointProbeConfiguration(name, scopeType, measurementStrategy, warmupDelay);
        } else
            return load(name, null, element, context);
    }

    private MeasurementStrategyConfiguration loadMeasurementStrategy(String name, JsonObject element, ILoadContext context) {
        String type = getType(element);

        if (type.equals("ExternalMeasurementStrategy")) {
            boolean enabled = element.get("enabled");
            long warmupDelay = element.get("warmupDelay");
            return new ExternalMeasurementStrategyConfiguration(name, enabled, warmupDelay);
        } else if (type.equals("CompositeMeasurementStrategy")) {
            boolean allowing = element.get("allowing");
            String typeStr = element.get("type");
            Type strategyType;
            if (typeStr.equals("and"))
                strategyType = Type.AND;
            else if (typeStr.equals("or"))
                strategyType = Type.OR;
            else
                strategyType = Assert.error();

            List<MeasurementStrategyConfiguration> strategies = new ArrayList<MeasurementStrategyConfiguration>();
            for (Map.Entry<String, Object> entry : (JsonObject) element.get("strategies"))
                strategies.add(loadMeasurementStrategy(entry.getKey(), (JsonObject) entry.getValue(), context));

            return new CompositeMeasurementStrategyConfiguration(name, allowing, strategyType, strategies);
        } else if (type.equals("HighMemoryMeasurementStrategy")) {
            long estimationPeriod = element.get("estimationPeriod");
            double threshold = element.get("threshold");
            return new HighMemoryMeasurementStrategyConfiguration(name, estimationPeriod, threshold);
        } else if (type.equals("HighCpuMeasurementStrategy")) {
            long estimationPeriod = element.get("estimationPeriod");
            double threshold = element.get("threshold");
            return new HighCpuMeasurementStrategyConfiguration(name, estimationPeriod, threshold);
        } else if (type.equals("CheckPointMeasurementStrategy")) {
            boolean allowing = element.get("allowing");

            QualifiedMethodFilter methodFilter = load(null, "QualifiedMethodFilter", (JsonObject) element.get("intercepted", null), context);
            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            InterceptPointcut pointcut = new InterceptPointcut(name + "-MeasurementStrategy", methodFilter, Enums.of(Kind.ENTER),
                    new StaticInterceptorConfiguration(CheckPointMeasurementStrategyInterceptor.class), false, false, 0);
            instrumentationContext.addPointcut(pointcut);

            return new CheckPointMeasurementStrategyConfiguration(name, allowing);
        } else
            return load(name, type, element, context);
    }

    private List<StackCounterConfiguration> loadStackCounters(JsonArray array, ILoadContext context) {
        if (array == null)
            return Collections.emptyList();

        List<StackCounterConfiguration> list = new ArrayList<StackCounterConfiguration>();
        for (Object object : array) {
            JsonObject element = (JsonObject) object;
            String type = getType(element);

            if (type.equals("AppStackCounter")) {
                boolean enabled = element.get("enabled");
                List<FieldConfiguration> fields = loadFields((JsonObject) element.get("fields", null), context);
                AppStackCounterType counterType = loadStackCounterType((String) element.get("type"));
                list.add(new AppStackCounterConfiguration(enabled, fields, counterType));
            } else
                list.add((StackCounterConfiguration) load(null, type, element, context));
        }

        return list;
    }

    private TimeSource loadTimeSource(String element) {
        String[] types = new String[]{"wallTime", "threadCpuTime"};

        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(element))
                return TimeSource.values()[i];
        }

        throw new InvalidConfigurationException();
    }

    private AppStackCounterType loadStackCounterType(String element) {
        String[] types = new String[]{"wallTime", "sysTime", "userTime", "waitTime", "waitCount", "blockTime",
                "blockCount", "gcCount", "gcTime",
                "allocationBytes", "allocationCount", "errorsCount",
                "threadsCount", "classesCount", "ioCount", "ioTime", "ioBytes", "fileCount", "fileTime", "fileBytes",
                "fileReadCount", "fileReadTime", "fileReadBytes", "fileWriteCount", "fileWriteTime", "fileWriteBytes",
                "netCount", "netTime", "netBytes", "netConnectCount", "netConnectTime",
                "netReceiveCount", "netReceiveTime", "netReceiveBytes", "netSendCount", "netSendTime", "netSendBytes",
                "dbTime", "dbConnectCount", "dbConnectTime", "dbQueryCount", "dbQueryTime"};

        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(element))
                return AppStackCounterType.values()[i];
        }

        throw new InvalidConfigurationException();
    }

    private Set<DumpType> loadDump(JsonArray array) {
        if (array == null)
            return Enums.noneOf(DumpType.class);

        Set<DumpType> set = Enums.noneOf(DumpType.class);
        for (Object object : array) {
            DumpType type = loadDumpType((String) object);
            set.add(type);
        }

        return set;
    }

    private DumpType loadDumpType(String element) {
        String[] types = new String[]{"state", "fullState", "measurements"};

        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(element))
                return DumpType.values()[i];
        }

        throw new InvalidConfigurationException();
    }

    private List<FieldConfiguration> loadFields(JsonObject elements, ILoadContext context) {
        if (elements == null)
            return Arrays.<FieldConfiguration>asList(new StandardFieldConfiguration());

        List<FieldConfiguration> fields = new ArrayList<FieldConfiguration>();
        for (Map.Entry<String, Object> entry : elements) {
            JsonObject element = (JsonObject) entry.getValue();

            FieldConfiguration configuration = load(entry.getKey(), null, element, context);
            fields.add(configuration);
        }

        return fields;
    }
}
