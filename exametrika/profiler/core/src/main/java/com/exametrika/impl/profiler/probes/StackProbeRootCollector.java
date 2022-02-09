/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;


import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.values.INameValue;
import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.profiler.IProfilerMXBean;
import com.exametrika.api.profiler.config.StackProbeConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.meters.Meters;
import com.exametrika.impl.aggregator.common.model.CallPath;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.aggregator.common.values.StackValue;
import com.exametrika.impl.profiler.scopes.Scope;
import com.exametrika.impl.profiler.scopes.ScopeContext;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.aggregator.common.meters.ICounter;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.profiler.IDumpProvider;
import com.exametrika.spi.profiler.IProbeCollector;
import com.exametrika.spi.profiler.ITransactionInfo;


/**
 * The {@link StackProbeRootCollector} is an root stack probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class StackProbeRootCollector extends StackProbeCollector implements IProbeCollector, IDumpProvider {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(StackProbeRootCollector.class.getName() + ".log");
    private static final ILogger stateLogger = Loggers.get(StackProbeRootCollector.class.getName() + ".state");
    private static final ILogger fullStateLogger = Loggers.get(StackProbeRootCollector.class.getName() + ".fullState");
    private static final ILogger measurementsLogger = Loggers.get(StackProbeRootCollector.class.getName() + ".measurements");
    private final StackProbe probe;
    private final StackProbeConfiguration configuration;
    private final ProbeContext context;
    private final Scope scope;
    private final Container container;
    private final IMarker marker;
    private long nextExtractionTime;
    protected long totalOverhead;
    private long nextFullExtractionTime;
    private volatile long lastExtractionTime;
    private long nextEstimationTime;
    private long startEstimationThreadCpuTime;
    private long lastScopeEstimationTime;
    private long lastScopeExtractionTime;
    private long lastClassifyTime;
    private int targetHotspotCount;
    protected int hotspotCount;
    protected int totalHotspotCount;
    protected int totalCollectorsCount;
    protected int createdCollectorsCount;
    protected int removedCollectorsCount;
    protected int classifyId;
    protected double hotspotCoverage;
    private int invalidationCount;
    protected boolean hasMeasurementsInClassifyPeriod;
    protected Set<UltraFastMethod> ultraFastMethods = new HashSet<UltraFastMethod>();
    private TransactionInfo transaction = new TransactionInfo();
    private MeterContainer meters;
    private ICounter scopeTime;

    protected enum Extractor {
        THREAD_SUSPEND,
        SCOPE,
        SLOW_METHOD
    }

    public StackProbeRootCollector(StackProbe probe, StackProbeConfiguration configuration, ProbeContext context,
                                   Scope scope, Container container) {
        super(0, CallPath.root(), null, null, JsonUtils.EMPTY_OBJECT,
                new NameMeasurementId(scope.getName(), CallPath.root(), "app.stack.root"));

        Assert.notNull(probe);
        Assert.notNull(configuration);
        Assert.notNull(context);
        Assert.notNull(scope);
        Assert.notNull(container);

        this.probe = probe;
        this.configuration = configuration;
        this.context = context;
        this.scope = scope;
        this.container = container;

        meters = new MeterContainer(getId(), context, container.contextProvider);

        JsonObject metadata = Json.object()
                .put("node", context.getConfiguration().getNodeName())
                .toObject();
        meters.setMetadata(metadata);
        scopeTime = meters.addMeter("app.cpu.time", new CounterConfiguration(true, configuration.getFields(), true, 0), null);

        long currentTime = context.getTimeService().getCurrentTime();
        long extractionPeriod = configuration.getExtractionPeriod();
        lastExtractionTime = currentTime;
        nextExtractionTime = (currentTime / extractionPeriod + 1) * extractionPeriod;

        long estimationPeriod = configuration.getMaxEstimationPeriod();
        long estimatedCalibrateTime = probe.isCalibrated() ? 0 : (ScopeContext.CALIBRATE_DELAY + 40000);
        nextEstimationTime = currentTime + estimationPeriod + configuration.getWarmupDelay() + estimatedCalibrateTime;
        lastScopeEstimationTime = scope.getTotalTime(context.getTimeSource().getCurrentTime());
        lastScopeExtractionTime = lastScopeEstimationTime;
        lastClassifyTime = currentTime;

        targetHotspotCount = configuration.getMinHotspotCount();
        hotspotCoverage = 100;

        container.methodCounters = new long[context.getConfiguration().getMaxInstrumentedMethodsCount()];
        blocked = null;
        marker = Loggers.getMarker(scope.getName().toString(), Loggers.getMarker(container.thread.getName()));

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.estimationPeriodStarted(lastScopeEstimationTime));
    }

    public StackProbeConfiguration getConfiguration() {
        return configuration;
    }

    public Container getContainer() {
        return container;
    }

    public Scope getScope() {
        return scope;
    }

    public StackProbe getProbe() {
        return probe;
    }

    public ITransactionInfo getTransaction() {
        if (transaction.active)
            return transaction;
        else
            return null;
    }

    public void beginTransaction(String combineId, long transactionId, long transactionStartTime) {
        transaction.combineId = combineId;
        transaction.id = transactionId;
        transaction.startTime = transactionStartTime;
        transaction.active = true;
    }

    public long getScopeEstimationPeriod(long currentCpuTime) {
        return scope.getTotalTime(currentCpuTime) - lastScopeEstimationTime;
    }

    public ProbeContext getContext() {
        return context;
    }

    public IScopeName getScopeName() {
        return scope.getName();
    }

    public StackProbeCalibrateInfo getCalibrateInfo() {
        return probe.getCalibrateInfo();
    }

    public int getHotspotCount() {
        return targetHotspotCount;
    }

    public void checkOverhead(long currentCpuTime) {
        long scopeEstimationPeriod = getScopeEstimationPeriod(currentCpuTime);
        long threshold = (long) (scopeEstimationPeriod * configuration.getTolerableOverhead() * 5 / 100);
        if (totalOverhead > threshold && scopeEstimationPeriod >= 5000000000l) {
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, marker, messages.overheadExceeded(totalOverhead, threshold));

            classify();
        }
    }

    public void addUltraFastMethod(String name, String className, int index, long count, long duration) {
        ultraFastMethods.add(new UltraFastMethod(name, className, index, count, duration));
    }

    @Override
    public boolean isExtractionRequired() {
        if (startEstimationThreadCpuTime == 0 && probe.isEnabled())
            startEstimationThreadCpuTime = ManagementFactory.getThreadMXBean().getThreadCpuTime(container.thread.getId());

        long currentTime = context.getTimeService().getCurrentTime();
        if (blocked == null)
            return currentTime >= nextEstimationTime;
        else if (context.getMeasurementHandler().canHandle())
            return (currentTime > lastExtractionTime + configuration.getExtractionPeriod() * 5) ||
                    (currentTime > nextEstimationTime + configuration.getMaxEstimationPeriod());
        else
            return false;
    }

    @Override
    public void extract() {
        extract(Extractor.THREAD_SUSPEND);
    }

    @Override
    public void begin() {
        validate(false);

        container.setTop(this);

        if (lastScopeEstimationTime == 0)
            lastScopeEstimationTime = scope.getTotalTime(context.getTimeSource().getCurrentTime());
        if (lastScopeExtractionTime == 0)
            lastScopeExtractionTime = lastScopeEstimationTime;

        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.collectorActivated());
    }

    @Override
    public void end() {
        extract(Extractor.SCOPE);

        container.setTop(null);
        transaction.active = false;

        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.collectorDeactivated());
    }

    @Override
    public String toString() {
        return '[' + container.thread.getName() + ':' + scope.getName() + ":<root>]";
    }

    @Override
    public String getName() {
        return configuration.getName();
    }

    @Override
    public JsonObject dump(int flags) {
        long currentCpuTime;
        if (container.thread == Thread.currentThread())
            currentCpuTime = context.getTimeSource().getCurrentTime();
        else
            currentCpuTime = context.getTimeSource().getCurrentTime(container.thread.getId());

        long scopeTotalTime = scope.getTotalTime(currentCpuTime);
        long scopeEstimationPeriod = scopeTotalTime - lastScopeEstimationTime;
        return dump(flags, scopeEstimationPeriod);
    }

    @Override
    protected String getComponentType() {
        return "app.stack.root";
    }

    @Override
    protected boolean canExtract() {
        return false;
    }

    protected void extract(Extractor extractor) {
        long currentTime = context.getTimeService().getCurrentTime();

        if (scope.isPermanent() && !scope.getContainer().hasScopes() &&
                !validate(!hasMeasurementsInClassifyPeriod && currentTime >= nextEstimationTime)) {
            long estimationPeriod = configuration.getMaxEstimationPeriod();
            nextEstimationTime = (currentTime / estimationPeriod + 1) * estimationPeriod;
            return;
        }

        if (blocked != null && context.getMeasurementHandler().canHandle() && currentTime >= nextExtractionTime &&
                (scope.isPermanent() || extractor == Extractor.SCOPE || currentTime >= nextExtractionTime + configuration.getMaxEstimationPeriod())) {
            long currentCpuTime;
            if (container.thread == Thread.currentThread())
                currentCpuTime = context.getTimeSource().getCurrentTime();
            else
                currentCpuTime = context.getTimeSource().getCurrentTime(container.thread.getId());

            long scopeTotalTime = scope.getTotalTime(currentCpuTime);

            boolean force = false;
            if (nextFullExtractionTime > 0 && currentTime >= nextFullExtractionTime)
                force = true;

            long fullExtractionPeriod = context.getConfiguration().getFullExtractionPeriod();
            nextFullExtractionTime = (currentTime / fullExtractionPeriod + 1) * fullExtractionPeriod;

            long extractionPeriod = configuration.getExtractionPeriod();
            nextExtractionTime = (currentTime / extractionPeriod + 1) * extractionPeriod;
            long period = (lastExtractionTime != 0) ? currentTime - lastExtractionTime : extractionPeriod;

            if (measurementsLogger.isLogEnabled(LogLevel.TRACE))
                measurementsLogger.log(LogLevel.TRACE, marker, messages.measurementsDump(dump(IProfilerMXBean.MEASUREMENTS_FLAG |
                        IProfilerMXBean.FULL_STATE_FLAG)));

            int schemaVersion = context.getConfiguration().getSchemaVersion();
            List<Measurement> measurements = new ArrayList<Measurement>();

            if (scope.isPermanent()) {
                scopeTime.measureDelta(scopeTotalTime - lastScopeExtractionTime);
                Measurement measurement = meters.extract(period, 0, true, true);
                measurements.add(new Measurement(measurement.getId(), new ComponentValue(Arrays.asList(
                        new StackValue(((INameValue) measurement.getValue().getMetrics().get(0)).getFields(),
                                ((INameValue) measurement.getValue().getMetrics().get(0)).getFields())),
                        measurement.getValue().getMetadata()), period, null));
            }

            extract(currentTime, period, force, 0, measurements);
            lastExtractionTime = currentTime;
            lastScopeExtractionTime = scopeTotalTime;

            if (classifyId >= configuration.getExtractionDelayCount() && !measurements.isEmpty()) {
                MeasurementSet set = new MeasurementSet(measurements, null, schemaVersion, currentTime, 0);
                context.getStackMeasurementHandler().handle(set);
            }
        }

        if (currentTime >= nextEstimationTime)
            classify();
    }

    private JsonObject dump(int flags, long scopeEstimationPeriod) {
        Json json = Json.object();

        if ((flags & IProfilerMXBean.STATE_FLAG) != 0) {
            json.put("classify-id", classifyId)
                    .putIf("currentHotspotCount", hotspotCount, hotspotCount > 0)
                    .put("targetHotspotCount", targetHotspotCount)
                    .putIf("totalHotspotCount", totalHotspotCount, totalHotspotCount > 0)
                    .putIf("totalCollectorsCount", totalCollectorsCount, totalCollectorsCount > 0)
                    .put("hotspotCoverage", hotspotCoverage)
                    .putIf("createdCollectorsCount", createdCollectorsCount, createdCollectorsCount > 0)
                    .putIf("removedCollectorsCount", removedCollectorsCount, removedCollectorsCount > 0)
                    .put("period", scopeEstimationPeriod)
                    .put("totalOverheadPercentage", (double) totalOverhead * 100 / scopeEstimationPeriod);
        }

        if ((flags & IProfilerMXBean.MEASUREMENTS_FLAG) != 0)
            meters.dump(json.putArray("meters"), false, 0);

        dump(json.putObject("<root>"), flags, 0, scopeEstimationPeriod);
        return json.toObject();
    }

    private boolean validate(boolean force) {
        if (!force && invalidationCount == probe.getInvalidationCount())
            return true;

        invalidateChildren();

        if (container.methodCounters != null || probe.isEstimating())
            return true;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.stackInvalidated());

        if (!probe.isEnabled())
            blocked = null;
        else {
            blocked = Boolean.FALSE;
            invalidationCount = probe.getInvalidationCount();

            if (scope.isPermanent() && (probe.getStackMeasurementStrategy() == null || probe.getStackMeasurementStrategy().allow())) {
                container.setTop(recoverStack(-1, true));

                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, marker, messages.stackRecovered(Meters.shorten(((StackProbeCollector) container.top).getCallPath())));
            }
        }

        return false;
    }

    private void classify() {
        long currentTime = context.getTimeService().getCurrentTime();
        if (blocked == null && container.methodCounters != null) {
            container.initTraceSnapshot();
            long estimationPeriod = ManagementFactory.getThreadMXBean().getThreadCpuTime(container.thread.getId()) -
                    startEstimationThreadCpuTime;

            long totalCallCount = 0, filteredCallCount = 0, unfilteredCallCount = 0;
            long totalMethodCount = 0;
            TreeMap<Long, TIntList> map = new TreeMap<Long, TIntList>();
            for (int i = 0; i < container.methodCounters.length; i++) {
                long count = container.methodCounters[i];
                totalCallCount += count;

                if (count > 1000) {
                    TIntList list = map.get(count);
                    if (list == null) {
                        list = new TIntArrayList();
                        map.put(count, list);
                    }
                    list.add(i);
                } else
                    unfilteredCallCount += count;

                if (count > 0)
                    totalMethodCount++;
            }

            Set<UltraFastMethod> ultraFastMethods = new LinkedHashSet<UltraFastMethod>();

            long tolerableOverhead = (long) (estimationPeriod * configuration.getTolerableOverhead() / 100);
            long tolerableCallCount = tolerableOverhead / StackProbeCalibrateInfo.MIN_OVERHEAD;
            long filteredMethodCount = 0;
            if (totalCallCount > tolerableCallCount) {
                filteredCallCount = totalCallCount - tolerableCallCount;
                long sum = 0;
                for (Map.Entry<Long, TIntList> entry : map.descendingMap().entrySet()) {
                    long count = entry.getKey();
                    for (TIntIterator it = entry.getValue().iterator(); it.hasNext(); ) {
                        int index = it.next();

                        boolean filtered;
                        if (sum < filteredCallCount && sum + count > filteredCallCount) {
                            if (filteredCallCount - sum >= sum + count - filteredCallCount)
                                filtered = true;
                            else
                                filtered = false;
                        } else if (sum < filteredCallCount)
                            filtered = true;
                        else
                            filtered = false;

                        sum += count;

                        if (!filtered)
                            unfilteredCallCount += count;
                        else {
                            filteredMethodCount++;

                            IJoinPoint joinPoint = context.getJoinPointProvider().findJoinPoint(index, -1);
                            if (joinPoint != null)
                                ultraFastMethods.add(new UltraFastMethod(joinPoint.getClassName() + "." + joinPoint.getMethodSignature(),
                                        joinPoint.getClassName(), index, count, estimationPeriod / count));
                        }
                    }
                }
            } else
                unfilteredCallCount = totalCallCount;

            if (!ultraFastMethods.isEmpty())
                probe.addUltraFastMethods(ultraFastMethods);

            if (logger.isLogEnabled(LogLevel.DEBUG)) {
                logger.log(LogLevel.DEBUG, marker, messages.estimationPeriodFinished(estimationPeriod, totalCallCount,
                        tolerableCallCount, filteredCallCount, unfilteredCallCount, totalMethodCount, filteredMethodCount,
                        totalMethodCount - filteredMethodCount));
            }

            if (!scope.isPermanent())
                blocked = probe.isEnabled() ? Boolean.FALSE : null;

            container.methodCounters = null;

            estimationPeriod = configuration.getMaxEstimationPeriod();
            nextEstimationTime = (currentTime / estimationPeriod + 1) * estimationPeriod;
            validate(false);

            return;
        } else if (blocked == null)
            return;

        long currentCpuTime;
        if (container.thread == Thread.currentThread())
            currentCpuTime = context.getTimeSource().getCurrentTime();
        else
            currentCpuTime = context.getTimeSource().getCurrentTime(container.thread.getId());

        long scopeTotalTime = scope.getTotalTime(currentCpuTime);
        long scopeEstimationPeriod = scopeTotalTime - lastScopeEstimationTime;
        if (scopeEstimationPeriod < configuration.getMaxEstimationPeriod() * 1000000 / 10 &&
                (currentTime - lastClassifyTime) < configuration.getMaxEstimationPeriod() * 2)
            return;

        classifyId++;

        hotspotCoverage = 100;
        hotspotCount = 0;
        totalHotspotCount = 0;
        totalCollectorsCount = 0;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.classifyStarted(classifyId, currentCpuTime, scopeEstimationPeriod));

        if (stateLogger.isLogEnabled(LogLevel.TRACE))
            stateLogger.log(LogLevel.TRACE, marker, messages.stateDumpBefore(
                    dump(IProfilerMXBean.STATE_FLAG, scopeEstimationPeriod)));

        if (fullStateLogger.isLogEnabled(LogLevel.TRACE))
            fullStateLogger.log(LogLevel.TRACE, marker, messages.stateDumpBefore(
                    dump(IProfilerMXBean.FULL_STATE_FLAG, scopeEstimationPeriod)));

        if (measurementsLogger.isLogEnabled(LogLevel.TRACE))
            measurementsLogger.log(LogLevel.TRACE, marker, messages.measurementsDump(
                    dump(IProfilerMXBean.MEASUREMENTS_FLAG, scopeEstimationPeriod)));

        classify(currentCpuTime, scopeEstimationPeriod, 0);

        removedCollectorsCount = createdCollectorsCount - totalCollectorsCount;

        if (!ultraFastMethods.isEmpty()) {
            probe.addUltraFastMethods(ultraFastMethods);
            ultraFastMethods.clear();
        }

        if (hotspotCoverage < configuration.getHotspotCoverage()) {
            if (targetHotspotCount + configuration.getHotspotStep() <= configuration.getMaxHotspotCount())
                targetHotspotCount += configuration.getHotspotStep();
        } else {
            if (targetHotspotCount - configuration.getHotspotStep() >= configuration.getMinHotspotCount())
                targetHotspotCount -= configuration.getHotspotStep();
        }

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.classifyFinished(classifyId, hotspotCount, targetHotspotCount, hotspotCoverage,
                    (double) totalOverhead * 100 / scopeEstimationPeriod));

        if (stateLogger.isLogEnabled(LogLevel.TRACE))
            stateLogger.log(LogLevel.TRACE, marker, messages.stateDumpAfter(
                    dump(IProfilerMXBean.STATE_FLAG, scopeEstimationPeriod)));

        if (fullStateLogger.isLogEnabled(LogLevel.TRACE))
            fullStateLogger.log(LogLevel.TRACE, marker, messages.stateDumpAfter(
                    dump(IProfilerMXBean.FULL_STATE_FLAG, scopeEstimationPeriod)));

        totalOverhead = 0;
        lastScopeEstimationTime = scopeTotalTime;

        long estimationPeriod = configuration.getMaxEstimationPeriod();
        nextEstimationTime = (currentTime / estimationPeriod + 1) * estimationPeriod;
        lastClassifyTime = currentTime;
        hasMeasurementsInClassifyPeriod = false;
    }

    private static class TransactionInfo implements ITransactionInfo {
        private boolean active;
        private String combineId;
        private long id;
        private long startTime;

        @Override
        public String getCombineId() {
            return combineId;
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public long getStartTime() {
            return startTime;
        }
    }

    private interface IMessages {
        @DefaultMessage("State dump after classify:\n{0}")
        ILocalizedMessage stateDumpAfter(JsonObject dump);

        @DefaultMessage("Total overhead exceeded threshold. Total overhead: {0}, threshold: {1}")
        ILocalizedMessage overheadExceeded(long totalOverhead, long threshold);

        @DefaultMessage("Ultra-fast methods estimation period is started. Scope time: {0}")
        ILocalizedMessage estimationPeriodStarted(long lastScopeEstimationTime);

        @DefaultMessage("Stack probe root collector is activated.")
        ILocalizedMessage collectorActivated();

        @DefaultMessage("Stack probe root collector is deactivated.")
        ILocalizedMessage collectorDeactivated();

        @DefaultMessage("Stack is invalidated.")
        ILocalizedMessage stackInvalidated();

        @DefaultMessage("Stack is recovered. Top: {0}")
        ILocalizedMessage stackRecovered(String top);

        @DefaultMessage("Ultra-fast methods estimation period is finished; estimation period: {0}\n" +
                "    total calls:{1}, tolerable calls: {2}, filtered calls: {3}, unfiltered calls: {4}\n" +
                "    total methods: {5}, filtered methods: {6}, unfiltered methods: {7}")
        ILocalizedMessage estimationPeriodFinished(long estimationPeriod, long totalCallCount, long tolerableCallCount,
                                                   long filteredCallCount, long unfilteredCallCount, long totalMethodCount, long filteredMethodMethodCount,
                                                   long unfilteredMethodCount);

        @DefaultMessage("Classify {0} is started. cpu time: {1}, estimation period: {2}")
        ILocalizedMessage classifyStarted(int classifyId, long currentCpuTime, long scopeEstimationPeriod);

        @DefaultMessage("State dump before classify:\n{0}")
        ILocalizedMessage stateDumpBefore(JsonObject dump);

        @DefaultMessage("Measurements dump:\n{0}")
        ILocalizedMessage measurementsDump(JsonObject dump);

        @DefaultMessage("Classify {0} is finished. current hotspot count: {1}, target hotspot count: {2}, hotspot coverage: {3}, overhead percentage: {4}")
        ILocalizedMessage classifyFinished(int classifyId, int hotspotCount, int targetHotspotCount, double hotspotCoverage,
                                           double totalOverheadPercentage);
    }
}
