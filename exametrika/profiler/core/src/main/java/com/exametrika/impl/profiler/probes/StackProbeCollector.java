/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.instrument.IJoinPointProvider.JoinPointEntry;
import com.exametrika.api.profiler.IProfilerMXBean;
import com.exametrika.api.profiler.config.StackProbeConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.common.meters.Meters;
import com.exametrika.impl.aggregator.common.model.CallPath;
import com.exametrika.impl.aggregator.common.model.MeasurementIdProvider;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.aggregator.common.values.StackValue;
import com.exametrika.impl.profiler.boot.AgentStackProbeInterceptor;
import com.exametrika.impl.profiler.boot.AgentlessStackProbeInterceptor;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector.Extractor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.IFieldCollector;
import com.exametrika.spi.aggregator.common.meters.IGauge;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;
import com.exametrika.spi.profiler.boot.Collector;
import com.exametrika.spi.profiler.config.StackCounterConfiguration;

/**
 * The {@link StackProbeCollector} is an stack probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class StackProbeCollector extends Collector {
    public static final boolean CHECK_STACK = System.getProperty("com.exametrika.profiler.checkStack", "false").equals("true");
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(StackProbeCollector.class);
    private static final int HOTSPOT_FLAG = 0x1;
    private static final int NON_HOTSPOT_FLAG = 0x2;
    private static final int SLOW_FLAG = 0x4;
    private static final int FAST_FLAG = 0x8;
    private static final int SAMPLING_FLAG = 0x10;
    private static final int SAMPLING_ROOT_FLAG = 0x20;
    private static final int BEGIN_FLAG = 0x40;
    private static final int BEGIN_HOTSPOT_FLAG = 0x80;
    private static final int BEGIN_BLOCKED_FLAG = 0x100;
    private static final int UNKNOWN_STACK_FLAG = 0x200;
    private static final int HAS_MEASUREMENT_FLAG = 0x400;
    private static final int HAS_MEASUREMENTS_IN_CLASSIFY_PERIOD_FLAG = 0x800;
    private static final int NON_SAMPLING_ROOT_FLAG = 0x1000;
    private static final int PERMANENT_HOTSPOT_FLAG = 0x2000 | HOTSPOT_FLAG;
    private final int index;
    private final ICallPath callPath;
    private final NameMeasurementId id;
    private final StackProbeRootCollector root;
    private final StackProbeCollector parent;
    private final IMarker marker;
    private StackProbeCollector firstChild;
    private StackProbeCollector nextSibling;
    private final JsonObject metadata;
    private int flags;
    private StackProbeCollectorData data;
    private byte idleRetentionCount;

    public StackProbeCollector(int index, ICallPath callPath, StackProbeRootCollector root, StackProbeCollector parent,
                               JsonObject metadata, NameMeasurementId id) {
        if (root == null)
            root = (StackProbeRootCollector) this;

        Assert.notNull(callPath);
        Assert.isTrue((callPath.isEmpty()) == (root == this));
        Assert.isTrue((parent == null) == (root == this));

        this.index = index;
        this.callPath = callPath;

        this.root = root;
        this.parent = parent;
        this.blocked = parent != null ? null : Boolean.FALSE;
        this.flags = (parent != null) ? 0 : HOTSPOT_FLAG;
        this.metadata = metadata;

        if (id != null)
            this.id = id;
        else
            this.id = new NameMeasurementId(parent.id.getScope(),
                    ((ICallPath) parent.id.getLocation()).getChild(callPath.getLastSegment()), getComponentType());

        if (root != this)
            marker = Loggers.getMarker(Meters.shorten(callPath), Loggers.getMarker(root.getScope().getName().toString()),
                    Loggers.getMarker(root.getContainer().thread.getName()));
        else
            marker = null;

        root.createdCollectorsCount++;
    }

    public void init() {
        if (root != this) {
            long currentCpuTime;
            if (root.getContainer().thread == Thread.currentThread())
                currentCpuTime = root.getContext().getTimeSource().getCurrentTime();
            else
                currentCpuTime = root.getContext().getTimeSource().getCurrentTime(root.getContainer().thread.getId());

            createData(currentCpuTime);

            if (isPermanentHotspotCollector())
                setPermanentHotspot(true);
        }
    }

    @Override
    public String toString() {
        return callPath.toString();
    }

    public static void traceTop(StackProbeCollector oldTop, StackProbeCollector newTop) {
        if (logger.isLogEnabled(LogLevel.TRACE) && (oldTop != null || newTop != null)) {
            StackProbeCollector top = null;
            if (oldTop != null)
                top = oldTop;
            else
                top = newTop;

            IMarker marker = Loggers.getMarker(top.getRoot().getScope().getName().toString(),
                    Loggers.getMarker(top.getRoot().getContainer().thread.getName()));
            logger.log(LogLevel.TRACE, marker, messages.topChanged(oldTop, newTop));
        }
    }

    protected final StackProbeRootCollector getRoot() {
        return root;
    }

    protected final StackProbeCollector getParent() {
        return parent;
    }

    protected final ICallPath getCallPath() {
        return callPath;
    }

    protected final NameMeasurementId getId() {
        return id;
    }

    protected final JsonObject getMetadata() {
        return metadata;
    }

    protected final MeterContainer getMeters() {
        return data.meters;
    }

    protected final boolean isHotspot() {
        return (flags & HOTSPOT_FLAG) != 0;
    }

    protected final boolean isFast() {
        return (flags & FAST_FLAG) != 0;
    }

    protected final boolean isBlocked() {
        return blocked == null || (flags & BEGIN_BLOCKED_FLAG) != 0;
    }

    protected boolean isPermanentHotspotCollector() {
        return false;
    }

    protected final StackProbeCollector beginMeasure(int index, int version, IStackProbeCollectorFactory collectorFactory,
                                                     Object param, Class collectorClass) {
        if ((flags & BEGIN_BLOCKED_FLAG) != 0)
            return null;

        StackProbeCollector collector;
        if ((flags & UNKNOWN_STACK_FLAG) != 0) {
            collector = recoverStackBranch(index, param);
            if (collector != null) {
                if (collectorClass != null && !collectorClass.isAssignableFrom(collector.getClass()))
                    collector = collector.getChild(index, version, collectorFactory, param);
                else
                    collector.clearUnknownStackFlag();
            }
        } else
            collector = getChild(index, version, collectorFactory, param);

        if (CHECK_STACK && collectorClass == null)
            checkStack(collector);

        if (collector != null) {
            collector.beginMeasure(param);
            return collector;
        } else
            return null;
    }

    protected final StackProbeCollector getChild(int index, int version, IStackProbeCollectorFactory collectorFactory, Object param) {
        StackProbeCollector collector = findChild(index, param);

        if (collector == null)
            collector = createCollector(index, version, collectorFactory, param);

        return collector;
    }

    protected StackProbeCollector findChild(int index, Object param) {
        for (StackProbeCollector child = firstChild; child != null; child = child.nextSibling) {
            if (child.index == index)
                return child;
        }

        return null;
    }

    protected void beginMeasure(Object param) {
        if (blocked == null)
            flags |= BEGIN_BLOCKED_FLAG;

        if ((flags & NON_HOTSPOT_FLAG) != 0)
            return;

        flags |= BEGIN_FLAG;

        if ((flags & FAST_FLAG) != 0)
            beginFastMeasure(param);
        else
            beginNormalMeasure(param);
    }

    protected void beginFastMeasure(Object param) {
        if ((flags & HOTSPOT_FLAG) != 0) {
            flags |= BEGIN_HOTSPOT_FLAG;
            beginMeasureMeters(param);
        } else
            flags &= ~BEGIN_HOTSPOT_FLAG;

        long value = Times.getTickCount();

        data.beginValue = value;
        data.childrenTotalDelta = 0;
        data.beginTotalOverhead = root.totalOverhead;
    }

    protected void beginNormalMeasure(Object param) {
        long beginOverhead = beginMeasureOverhead();

        if ((flags & HOTSPOT_FLAG) != 0) {
            flags |= BEGIN_HOTSPOT_FLAG;
            beginMeasureMeters(param);
        } else
            flags &= ~BEGIN_HOTSPOT_FLAG;

        long value = root.getContext().getTimeSource().getCurrentTime();

        data.beginValue = value;
        data.childrenTotalDelta = 0;
        data.beginTotalOverhead = root.totalOverhead;

        endBeginMeasureOverhead(beginOverhead);
    }

    protected void endMeasure() {
        endMeasure(0);
    }

    protected void endMeasure(long totalDelta) {
        if ((flags & NON_HOTSPOT_FLAG) != 0 || (flags & BEGIN_FLAG) == 0) {
            flags &= ~(BEGIN_BLOCKED_FLAG | BEGIN_FLAG);
            return;
        }

        flags &= ~(BEGIN_BLOCKED_FLAG | BEGIN_FLAG);

        if ((flags & FAST_FLAG) != 0)
            endFastMeasure(totalDelta);
        else
            endNormalMeasure();
    }

    protected final void endFastMeasure(long totalDelta) {
        StackProbeCalibrateInfo info = root.getCalibrateInfo();

        long overheadDelta = root.totalOverhead - data.beginTotalOverhead;
        if (totalDelta == 0) {
            long value = Times.getTickCount();
            totalDelta = (long) ((value - data.beginValue) / Times.getTickFrequency()) - info.fastCollectorInnerOverhead -
                    overheadDelta;
        }

        if (totalDelta < data.childrenTotalDelta)
            totalDelta = data.childrenTotalDelta;

        long inherentDelta = totalDelta - data.childrenTotalDelta;

        data.count++;

        if (hasOutliers() && totalDelta > 2 * StackProbeCalibrateInfo.FAST_THRESHOLD) {
            data.outliersCount++;

            inherentDelta = data.inherent / data.count;
            totalDelta = data.total / data.count;
        }

        boolean disableInherent = false;
        if ((flags & SAMPLING_ROOT_FLAG) != 0) {
            if (blocked == null) {
                inherentDelta = 0;
                disableInherent = true;
            } else
                data.samplingTotal += totalDelta;

            updateSampleCount();
        }

        data.inherent += inherentDelta;
        data.total += totalDelta;

        if ((flags & HOTSPOT_FLAG) != 0) {
            data.measurementCount++;
            if (!disableInherent) {
                for (int i = 0; i < data.inherentFieldCollectors.length; i++)
                    data.inherentFieldCollectors[i].update(inherentDelta);
            }
            for (int i = 0; i < data.totalFieldCollectors.length; i++)
                data.totalFieldCollectors[i].update(totalDelta);

            flags |= HAS_MEASUREMENT_FLAG;
        }

        long outerOverhead = getOuterOverhead();

        if (parent.data != null) {
            parent.data.childrenTotalDelta += totalDelta;
            parent.data.totalChildrenOverhead += overheadDelta;
            parent.data.totalChildrenOverhead += outerOverhead;
        }

        root.totalOverhead += outerOverhead;

        if ((flags & BEGIN_HOTSPOT_FLAG) != 0)
            endMeasureMeters(disableInherent);

        data.beginValue = 0;

        if ((flags & HAS_MEASUREMENTS_IN_CLASSIFY_PERIOD_FLAG) == 0)
            setHasMeasurementsInClassifyPeriod();
        root.hasMeasurementsInClassifyPeriod = true;

        if ((flags & HOTSPOT_FLAG) == 0 && (data.count & 0x3FF) == 0)
            classifyPartially(root.getContext().getTimeSource().getCurrentTime());
    }

    protected final void endNormalMeasure() {
        long beginOverhead = beginMeasureOverhead();

        long value = root.getContext().getTimeSource().getCurrentTime();

        int oldFlags = flags;
        StackProbeCollectorData oldData = this.data;

        StackProbeCalibrateInfo info = root.getCalibrateInfo();

        long overheadDelta = root.totalOverhead - data.beginTotalOverhead;
        long totalDelta = value - data.beginValue - info.normalCollectorInnerOverhead - overheadDelta;
        if (totalDelta < data.childrenTotalDelta)
            totalDelta = data.childrenTotalDelta;

        long inherentDelta = totalDelta - data.childrenTotalDelta;

        boolean disableInherent = false;
        if ((flags & SAMPLING_ROOT_FLAG) != 0) {
            if (blocked == null) {
                inherentDelta = 0;
                disableInherent = true;
            } else
                data.samplingTotal += totalDelta;

            updateSampleCount();
        }

        data.count++;

        data.inherent += inherentDelta;
        data.total += totalDelta;

        if ((flags & HOTSPOT_FLAG) != 0) {
            data.measurementCount++;
            if (!disableInherent) {
                for (int i = 0; i < data.inherentFieldCollectors.length; i++)
                    data.inherentFieldCollectors[i].update(inherentDelta);
            }
            for (int i = 0; i < data.totalFieldCollectors.length; i++)
                data.totalFieldCollectors[i].update(totalDelta);

            flags |= HAS_MEASUREMENT_FLAG;
        }

        if (parent.data != null) {
            parent.data.childrenTotalDelta += totalDelta;
            parent.data.totalChildrenOverhead += overheadDelta;
        }

        if ((flags & BEGIN_HOTSPOT_FLAG) != 0)
            endMeasureMeters(disableInherent);

        data.beginValue = 0;

        if ((flags & HAS_MEASUREMENTS_IN_CLASSIFY_PERIOD_FLAG) == 0)
            setHasMeasurementsInClassifyPeriod();
        root.hasMeasurementsInClassifyPeriod = true;

        if ((flags & HOTSPOT_FLAG) == 0)
            classifyPartially(value);

        if ((flags & SLOW_FLAG) != 0) {
            root.extract(Extractor.SLOW_METHOD);
            root.checkOverhead(value);
        }

        endEndMeasureOverhead(beginOverhead, oldFlags, oldData);
    }

    protected long beginMeasureOverhead() {
        return Times.getTickCount();
    }

    protected void endBeginMeasureOverhead(long beginTickCount) {
        long value = Times.getTickCount() - beginTickCount;
        if (value < 0)
            value = 0;
        data.beginSelfOverheadTickCount = value;
    }

    protected void endEndMeasureOverhead(long beginTickCount, int flags, StackProbeCollectorData data) {
        StackProbeCalibrateInfo info = root.getCalibrateInfo();

        long average;
        if ((flags & HOTSPOT_FLAG) != 0)
            average = getNormalCollectorFullOuterOverhead(false);
        else
            average = info.normalCollectorEstimatingOuterOverhead;

        long value;
        if (Times.isTickCountAvaliable()) {
            long threshold = average << 2;

            value = (long) ((Times.getTickCount() - beginTickCount + data.beginSelfOverheadTickCount) / Times.getTickFrequency());
            if (value <= 0 || value > threshold)
                value = average;
        } else
            value = average;

        root.totalOverhead += value;

        if (this.data == null)
            return;

        if (parent.data != null)
            parent.data.totalChildrenOverhead += value;
    }

    protected void beginMeasureMeters(Object param) {
        boolean slowEnabled = (flags & FAST_FLAG) == 0 && (flags & SAMPLING_FLAG) == 0;

        if (data.stackCounters != null) {
            for (int i = 0; i < data.stackCounters.size(); i++) {
                StackCounter stackCounter = data.stackCounters.get(i);
                if (!stackCounter.getConfiguration().isFast()) {
                    stackCounter.setEnabled(slowEnabled);
                    if (!slowEnabled)
                        continue;
                } else
                    stackCounter.setEnabled(true);

                stackCounter.beginMeasure();
            }
        }

        if (data.concurrencyLevel != null) {
            data.concurrencyLevelEnabled = slowEnabled;
            if (slowEnabled)
                data.concurrencyLevel.measure(root.getProbe().incrementConcurrency(index));
        }
    }

    protected void endMeasureMeters(boolean disableInherent) {
        if (data.stackCounters != null) {
            for (int i = 0; i < data.stackCounters.size(); i++) {
                StackCounter stackCounter = data.stackCounters.get(i);
                if (!stackCounter.isEnabled())
                    continue;

                boolean disable = disableInherent;
                if (disableInherent && !stackCounter.getConfiguration().isFast())
                    disable = false;

                stackCounter.endMeasure(disable, parent.data != null ? parent.data.stackCounters.get(stackCounter.getIndex()) : null);
            }
        }

        if (data.concurrencyLevel != null && data.concurrencyLevelEnabled)
            root.getProbe().decrementConcurrency(index);
    }

    protected final void classify(long currentCpuTime, long scopeEstimationPeriod, double approximationMultiplier) {
        root.totalCollectorsCount++;

        if ((flags & NON_HOTSPOT_FLAG) != 0) {
            flags &= ~NON_HOTSPOT_FLAG;
            if ((parent.flags & (SAMPLING_FLAG | SAMPLING_ROOT_FLAG)) != 0)
                flags |= SAMPLING_FLAG;

            createData(currentCpuTime);
            return;
        }

        double childrenApproximationMultiplier;
        if (this != root && (flags & SAMPLING_ROOT_FLAG) != 0 && data.samplingTotal > 0) {
            childrenApproximationMultiplier = (double) data.total / data.samplingTotal;

            long overheadDelta = (long) ((childrenApproximationMultiplier - 1) * data.totalChildrenOverhead);
            incrementTotalChildrenOverhead(overheadDelta);
        } else
            childrenApproximationMultiplier = approximationMultiplier;

        if (approximationMultiplier > 0) {
            data.total *= approximationMultiplier;
            data.count *= approximationMultiplier;
            data.outliersCount *= approximationMultiplier;
        }

        if (childrenApproximationMultiplier > 0) {
            data.inherent *= childrenApproximationMultiplier;
            data.totalChildrenOverhead *= childrenApproximationMultiplier;
        }

        long beforeOuterOverhead = getOuterOverhead();
        flags &= ~(SAMPLING_ROOT_FLAG | SAMPLING_FLAG | HOTSPOT_FLAG | PERMANENT_HOTSPOT_FLAG);

        StackProbeCollector prevCollector = null;
        for (StackProbeCollector collector = firstChild; collector != null; collector = collector.nextSibling) {
            boolean hasMeasurements = (collector.flags & HAS_MEASUREMENTS_IN_CLASSIFY_PERIOD_FLAG) != 0;
            collector.classify(currentCpuTime, scopeEstimationPeriod, childrenApproximationMultiplier);
            if (!hasMeasurements) {
                collector.idleRetentionCount++;
                if (collector.idleRetentionCount >= root.getConfiguration().getIdleRetentionCount()) {
                    if (prevCollector == null)
                        firstChild = collector.nextSibling;
                    else
                        prevCollector.nextSibling = collector.nextSibling;

                    removeCollector(collector);
                } else
                    prevCollector = collector;
            } else {
                collector.idleRetentionCount = 0;
                prevCollector = collector;
            }
        }

        if (this == root)
            return;

        if (isPermanentHotspotCollector()) {
            setPermanentHotspot(false);

            if (!hasChildrenHotspots())
                root.hotspotCount++;

            if (logger.isLogEnabled(LogLevel.TRACE))
                logger.log(LogLevel.TRACE, marker, messages.permanentHotspotDetected());
        }

        if ((flags & HOTSPOT_FLAG) == 0) {
            boolean ultraFast = checkUltraFast();

            if (!ultraFast && data.total >= (scopeEstimationPeriod - root.totalOverhead) / root.getHotspotCount()) {
                blocked = Boolean.FALSE;
                flags |= HOTSPOT_FLAG;
                setParentHotspot();

                if (!hasChildrenHotspots())
                    root.hotspotCount++;

                if (logger.isLogEnabled(LogLevel.TRACE)) {
                    double coverage = (double) data.total / (scopeEstimationPeriod - root.totalOverhead) * 100;
                    logger.log(LogLevel.TRACE, marker, messages.hotspotDetected(data.count, data.total, approximationMultiplier,
                            scopeEstimationPeriod, root.totalOverhead, coverage));
                }
            } else {
                double coverage = (double) data.total / (scopeEstimationPeriod - root.totalOverhead) * 100;
                root.hotspotCoverage -= coverage;
                incrementTotalChildrenOverhead(data.count * -beforeOuterOverhead);

                if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, marker, messages.nonHotspotDetected(data.count, data.total, approximationMultiplier,
                            scopeEstimationPeriod, root.totalOverhead, coverage));

                setNonHotspot();
                return;
            }
        }

        if ((flags & HOTSPOT_FLAG) != 0)
            root.totalHotspotCount++;

        if (data.count > 0 && data.total / data.count >= StackProbeCalibrateInfo.SLOW_THRESHOLD)
            flags |= SLOW_FLAG;
        else
            flags &= ~SLOW_FLAG;

        StackProbeCalibrateInfo info = root.getCalibrateInfo();
        double tolerableOverhead = data.total * root.getConfiguration().getTolerableOverhead() / 100;
        if (Times.isTickCountAvaliable()) {
            if (data.count > 0 && (data.total / data.count <= StackProbeCalibrateInfo.FAST_THRESHOLD ||
                    data.count * getNormalCollectorFullOuterOverhead(true) > tolerableOverhead)) {
                beginValueToTicks(currentCpuTime);
                flags |= FAST_FLAG;
            } else {
                beginValueToMillis(currentCpuTime);
                flags &= ~FAST_FLAG;
            }
        }

        long outerOverhead = getOuterOverhead();
        incrementTotalChildrenOverhead(data.count * (outerOverhead - beforeOuterOverhead));

        long selfOverhead = data.count * outerOverhead;

        if (selfOverhead > tolerableOverhead)
            parent.classifySamplingRoot(info, scopeEstimationPeriod);

        data.inherent = 0;
        data.total = 0;
        data.samplingTotal = 0;
        data.totalChildrenOverhead = 0;
        data.count = 0;
        data.outliersCount = 0;
        data.beginTotalOverhead = 0;

        flags &= ~HAS_MEASUREMENTS_IN_CLASSIFY_PERIOD_FLAG;
    }

    protected final void classifyPartially(long currentCpuTime) {
        long estimationPeriod = root.getScope().getTotalTime(currentCpuTime) - data.startEstimationScopeTime;
        if (estimationPeriod < root.getConfiguration().getMinEstimationPeriod() * 1000000)
            return;

        double approximationMultiplier = getParentApproximationMultiplier();

        boolean ultraFast = checkUltraFast();

        StackProbeCalibrateInfo info = root.getCalibrateInfo();

        long beforeOuterOverhead = getOuterOverhead();
        if (ultraFast || data.total * approximationMultiplier < (estimationPeriod -
                (root.totalOverhead - data.startEstimationRootTotalOverhead)) / root.getHotspotCount()) {
            double coverage = data.total * approximationMultiplier / (estimationPeriod - (root.totalOverhead -
                    data.startEstimationRootTotalOverhead)) * 100;
            root.hotspotCoverage -= coverage;
            incrementTotalChildrenOverhead(data.count * -beforeOuterOverhead);
            flags = (flags & BEGIN_BLOCKED_FLAG) | NON_HOTSPOT_FLAG;
            blocked = null;

            if (logger.isLogEnabled(LogLevel.TRACE))
                logger.log(LogLevel.TRACE, marker, messages.nonHotspotDetected((long) (data.count * approximationMultiplier),
                        (long) (data.total * approximationMultiplier), approximationMultiplier,
                        estimationPeriod, root.totalOverhead - data.startEstimationRootTotalOverhead, coverage));

            clearData();
            return;
        }

        flags |= HOTSPOT_FLAG;
        blocked = Boolean.FALSE;
        setParentHotspot();

        if (logger.isLogEnabled(LogLevel.TRACE)) {
            double coverage = data.total * approximationMultiplier / (estimationPeriod - (root.totalOverhead -
                    data.startEstimationRootTotalOverhead)) * 100;
            logger.log(LogLevel.TRACE, marker, messages.hotspotDetected((long) (data.count * approximationMultiplier),
                    (long) (data.total * approximationMultiplier), approximationMultiplier,
                    estimationPeriod, root.totalOverhead - data.startEstimationRootTotalOverhead, coverage));
        }

        double tolerableOverhead = data.total * root.getConfiguration().getTolerableOverhead() / 100;

        if (Times.isTickCountAvaliable() && (flags & FAST_FLAG) == 0 &&
                (data.total / data.count <= StackProbeCalibrateInfo.FAST_THRESHOLD ||
                        data.count * getNormalCollectorFullOuterOverhead(true) > tolerableOverhead))
            flags |= FAST_FLAG;

        long outerOverhead = getOuterOverhead();
        incrementTotalChildrenOverhead(data.count * (outerOverhead - beforeOuterOverhead));

        long selfOverhead = data.count * outerOverhead;
        if (selfOverhead > tolerableOverhead)
            parent.classifySamplingRoot(info, estimationPeriod);
    }

    protected void classifySamplingRoot(StackProbeCalibrateInfo info, long estimationPeriod) {
        if (this == root || data.count < 10 || (flags & UNKNOWN_STACK_FLAG) != 0 || (flags & NON_SAMPLING_ROOT_FLAG) != 0)
            return;

        StackProbeCollector collector = this;
        while (collector != root) {
            if ((collector.flags & SAMPLING_FLAG) == 0) {
                double tolerableOverhead = collector.data.total * root.getConfiguration().getTolerableOverhead() / 100;
                long selfOverhead = collector.data.count * collector.getOuterOverhead();
                if ((selfOverhead < tolerableOverhead / 2) ||
                        collector.parent == root || collector.parent.data.count < 10 || (collector.parent.flags & UNKNOWN_STACK_FLAG) != 0 ||
                        (collector.parent.flags & NON_SAMPLING_ROOT_FLAG) != 0) {
                    if (collector.initSampleCount(tolerableOverhead, selfOverhead, info, estimationPeriod)) {
                        collector.flags |= SAMPLING_ROOT_FLAG;
                        collector.setChildrenSamplingFlag();
                    }
                    break;
                }
            }

            collector = collector.parent;
        }
    }

    protected boolean canExtract() {
        return true;
    }

    protected void extract(long time, long period, boolean force, double approximationMultiplier, List<Measurement> measurements) {
        if ((flags & NON_HOTSPOT_FLAG) != 0)
            return;

        double childrenApproximationMultiplier = approximationMultiplier;
        if (canExtract()) {
            if ((flags & SAMPLING_ROOT_FLAG) != 0 && data.samplingTotal > 0)
                childrenApproximationMultiplier = (double) data.total / data.samplingTotal;
        }

        List<Measurement> childrenMeasurements = null;
        for (StackProbeCollector collector = firstChild; collector != null; collector = collector.nextSibling) {
            if (childrenMeasurements == null)
                childrenMeasurements = new ArrayList<Measurement>();
            collector.extract(time, period, force, childrenApproximationMultiplier, childrenMeasurements);
        }

        if (canExtract()) {
            boolean hasValue = false;
            List<IMetricValue> metrics = null;
            if ((flags & HAS_MEASUREMENT_FLAG) != 0) {
                hasValue = true;
                metrics = new ArrayList<IMetricValue>();
                List<IFieldValue> inherentFields = new ArrayList<IFieldValue>(data.inherentFieldCollectors.length);
                for (int i = 0; i < data.inherentFieldCollectors.length; i++)
                    inherentFields.add(data.inherentFieldCollectors[i].extract(data.measurementCount,
                            childrenApproximationMultiplier, true));

                List<IFieldValue> totalFields = new ArrayList<IFieldValue>(data.totalFieldCollectors.length);
                for (int i = 0; i < data.totalFieldCollectors.length; i++)
                    totalFields.add(data.totalFieldCollectors[i].extract(0, approximationMultiplier, true));

                metrics.add(new StackValue(inherentFields, totalFields));

                extractStackCounters(data.measurementCount, childrenApproximationMultiplier, approximationMultiplier, true, metrics);

                extractMeters(period, force, approximationMultiplier, metrics);

                data.measurementCount = 0;
                flags &= ~HAS_MEASUREMENT_FLAG;
            } else
                data.meters.extractLogs();

            if (childrenMeasurements != null && !childrenMeasurements.isEmpty())
                hasValue = true;

            if ((flags & HOTSPOT_FLAG) != 0 && hasValue) {
                if (metrics == null) {
                    metrics = new ArrayList<IMetricValue>();
                    metrics.add(null);

                    if (data.stackCounters != null) {
                        for (int i = 0; i < data.stackCounters.size(); i++)
                            metrics.add(null);
                    }

                    extractMeters(period, force, approximationMultiplier, metrics);
                }

                Measurement measurement = new Measurement(data.meters.getId(), new ComponentValue(metrics, metadata), period, null);
                measurements.add(measurement);
            }
        }

        if (childrenMeasurements != null && !childrenMeasurements.isEmpty())
            measurements.addAll(childrenMeasurements);
    }

    protected void extractMeters(long period, boolean force, double approximationMultiplier, List<IMetricValue> metrics) {
        data.meters.extract(period, approximationMultiplier, force, true, metrics);
    }

    protected final void extractStackCounters(long count, double inherentApproximationMultiplier, double totalApproximationMultiplier,
                                              boolean clear, List<IMetricValue> metrics) {
        if (data.stackCounters == null)
            return;

        for (StackCounter stackCounter : data.stackCounters) {
            boolean extractMeasurements = (((flags & FAST_FLAG) == 0 && (flags & SAMPLING_FLAG) == 0) ||
                    stackCounter.getConfiguration().isFast());

            if (!stackCounter.getConfiguration().isFast() && (flags & SAMPLING_ROOT_FLAG) != 0)
                inherentApproximationMultiplier = totalApproximationMultiplier;

            IMetricValue metric = stackCounter.extract(count, inherentApproximationMultiplier,
                    totalApproximationMultiplier, clear);

            if (extractMeasurements)
                metrics.add(metric);
            else
                metrics.add(null);
        }
    }

    protected StackProbeCollector createCollector(int index, int version, IStackProbeCollectorFactory collectorFactory,
                                                  Object param) {
        StackProbeCollector collector = collectorFactory.createCollector(index, version, this, param);
        if (collector == null)
            return null;

        collector.init();

        collector.nextSibling = firstChild;
        firstChild = collector;
        if ((flags & (SAMPLING_FLAG | SAMPLING_ROOT_FLAG)) != 0)
            collector.flags |= SAMPLING_FLAG;
        if ((flags & FAST_FLAG) != 0)
            collector.flags |= FAST_FLAG;

        return collector;
    }

    protected void createData(long currentCpuTime) {
        data = new StackProbeCollectorData();
        data.inherentFieldCollectors = Meters.createFieldCollectors(root.getConfiguration().getFields(),
                root.getContainer().contextProvider, new MeasurementIdProvider(id));
        data.totalFieldCollectors = Meters.createFieldCollectors(root.getConfiguration().getFields(),
                root.getContainer().contextProvider, new MeasurementIdProvider(id));
        data.meters = new MeterContainer(id, root.getContext(), root.getContainer().contextProvider);
        data.startEstimationScopeTime = root.getScope().getTotalTime(currentCpuTime);
        data.startEstimationRootTotalOverhead = root.totalOverhead;

        if (allowConcurrency() && root.getConfiguration().getConcurrencyLevel().isEnabled())
            data.concurrencyLevel = data.meters.addMeter("app.concurrency", root.getConfiguration().getConcurrencyLevel(), null);

        createMeters();
        createStackCounters();

        data.meters.setMetadata(metadata);

        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.hotspotEstimationStarted(data.startEstimationScopeTime));
    }

    protected String getComponentType() {
        return root.getConfiguration().getComponentType();
    }

    protected void clearData() {
        clearMeters();
        data = null;
        firstChild = null;
    }

    protected final void createStackCounters() {
        StackProbeConfiguration configuration = root.getConfiguration();
        for (StackCounterConfiguration stackCounterConfiguration : configuration.getStackCounters()) {
            if (!stackCounterConfiguration.isEnabled())
                continue;

            if (data.stackCounters == null)
                data.stackCounters = new ArrayList<StackCounter>();

            IInstanceContextProvider contextProvider = root.getContainer().contextProvider;
            StackCounter stackCounter = new StackCounter(stackCounterConfiguration, data.stackCounters.size(),
                    Meters.createFieldCollectors(stackCounterConfiguration.getFields(), contextProvider, data.meters.getIdProvider()),
                    Meters.createFieldCollectors(stackCounterConfiguration.getFields(), contextProvider, data.meters.getIdProvider()),
                    stackCounterConfiguration.createProvider(root.getContext()));

            data.stackCounters.add(stackCounter);
        }
    }

    protected void createMeters() {
    }

    protected void clearMeters() {
    }

    protected void removeCollector(StackProbeCollector collector) {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, marker, messages.collectorRemoved(collector.toString()));
    }

    protected void dump(Json json, int dumpFlags, double approximationMultiplier, long period) {
        double childrenApproximationMultiplier;
        if ((flags & SAMPLING_ROOT_FLAG) != 0 && data.samplingTotal > 0)
            childrenApproximationMultiplier = (double) data.total / data.samplingTotal;
        else
            childrenApproximationMultiplier = approximationMultiplier;

        boolean full = (dumpFlags & IProfilerMXBean.FULL_STATE_FLAG) == IProfilerMXBean.FULL_STATE_FLAG;

        if (parent != null) {
            if ((dumpFlags & IProfilerMXBean.STATE_FLAG) != 0) {
                json.put("index", index);

                StringBuilder builder = new StringBuilder();
                builder.append('[');
                if ((flags & PERMANENT_HOTSPOT_FLAG) == PERMANENT_HOTSPOT_FLAG)
                    builder.append("permanent-hotspot");
                else if ((flags & HOTSPOT_FLAG) != 0)
                    builder.append("hotspot");
                else if ((flags & NON_HOTSPOT_FLAG) != 0)
                    builder.append("non-hotspot");
                else
                    builder.append("estimating");

                if (blocked == null)
                    builder.append(",blocked");
                if ((flags & SLOW_FLAG) != 0)
                    builder.append(",slow");
                if ((flags & UNKNOWN_STACK_FLAG) != 0)
                    builder.append(",unknown-stack");
                if ((flags & FAST_FLAG) != 0)
                    builder.append(",fast");
                if ((flags & SAMPLING_FLAG) != 0)
                    builder.append(",sampling");
                if ((flags & SAMPLING_ROOT_FLAG) != 0)
                    builder.append(",sampling-root");
                if ((flags & NON_SAMPLING_ROOT_FLAG) != 0)
                    builder.append(",non-sampling-root");

                builder.append(']');

                json.putIf("metadata", metadata, metadata != null && full)
                        .put("flags", builder.toString());

                if (data != null && data.count > 0) {
                    long selfOverhead = data.count * getOuterOverhead();
                    long total = data.total;
                    long inherent = data.inherent;
                    long count = data.count;
                    long measurementCount = data.measurementCount;
                    long outliersCount = data.outliersCount;
                    if (approximationMultiplier > 0) {
                        total *= approximationMultiplier;
                        count *= approximationMultiplier;
                        measurementCount *= approximationMultiplier;
                        outliersCount *= approximationMultiplier;
                    }
                    if (childrenApproximationMultiplier > 0)
                        inherent *= childrenApproximationMultiplier;

                    double overheadPercentage = (double) (selfOverhead + data.totalChildrenOverhead) * 100 / total;

                    json.put("inherent", inherent)
                            .put("inherent%", (double) inherent * 100 / total)
                            .put("inherentAverage", (double) inherent / count)
                            .put("total", total)
                            .putIf("total%", (double) total * 100 / period, period != 0)
                            .put("totalAverage", (double) data.total / data.count)
                            .put("count", count)
                            .put("measurementCount", measurementCount)
                            .put("overheadPercentage", overheadPercentage)
                            .put("selfOverhead", selfOverhead)
                            .put("totalChildrenOverhead", data.totalChildrenOverhead)
                            .put("totalChildrenOverheadPerCall", data.totalChildrenOverhead / data.count)
                            .putIf("approximationMultiplier", approximationMultiplier, approximationMultiplier > 0)
                            .putIf("childrenApproximationMultiplier", childrenApproximationMultiplier, childrenApproximationMultiplier > 0)
                            .putIf("outliers", outliersCount, outliersCount > 0);
                }

                json.putIf("idleRetentionCount", idleRetentionCount, idleRetentionCount > 0);

                if (data != null && data.onSampleCount > 0 && (flags & SAMPLING_ROOT_FLAG) != 0) {
                    json.putIf("sampleCount", data.sampleCount, data.sampleCount > 0)
                            .putIf("onSampleCount", data.onSampleCount, data.onSampleCount > 0)
                            .putIf("offSampleCount", data.offSampleCount, data.offSampleCount > 0);
                }
            }

            if ((dumpFlags & IProfilerMXBean.MEASUREMENTS_FLAG) != 0 && data != null && (flags & HAS_MEASUREMENT_FLAG) != 0) {
                List<String> metricTypes = new ArrayList<String>();
                List<IMetricValue> metrics = new ArrayList<IMetricValue>();

                List<IFieldValue> inherentFields = new ArrayList<IFieldValue>(data.inherentFieldCollectors.length);
                for (int i = 0; i < data.inherentFieldCollectors.length; i++)
                    inherentFields.add(data.inherentFieldCollectors[i].extract(data.measurementCount,
                            childrenApproximationMultiplier, false));

                List<IFieldValue> totalFields = new ArrayList<IFieldValue>(data.totalFieldCollectors.length);
                for (int i = 0; i < data.totalFieldCollectors.length; i++)
                    totalFields.add(data.totalFieldCollectors[i].extract(0, approximationMultiplier, false));

                metricTypes.add("app.cpu.time");
                metrics.add(new StackValue(inherentFields, totalFields));

                if (data.stackCounters != null) {
                    extractStackCounters(data.measurementCount, childrenApproximationMultiplier, approximationMultiplier, false, metrics);
                    for (StackCounter stackCounter : data.stackCounters)
                        metricTypes.add(stackCounter.getConfiguration().getMetricType());
                }

                data.meters.extract(period, approximationMultiplier, true, false, metrics);
                data.meters.buildMetricTypes(metricTypes);

                Assert.checkState(metrics.size() == metricTypes.size());
                Json jsonMeters = json.putObject("meters");
                for (int i = 0; i < metrics.size(); i++) {
                    if (metrics.get(i) != null)
                        jsonMeters.put(metricTypes.get(i), metrics.get(i).toJson());
                }
            }
        }

        for (StackProbeCollector collector = firstChild; collector != null; collector = collector.nextSibling) {
            if ((collector.flags & HOTSPOT_FLAG) != 0 || full)
                collector.dump(json.putObject(collector.getCallPath().getLastSegment().toString()), dumpFlags,
                        childrenApproximationMultiplier, period);
        }
    }

    protected final void invalidateChildren() {
        firstChild = null;
    }

    protected void setCalibrate(boolean hotspot, boolean fast) {
        flags |= (hotspot ? HOTSPOT_FLAG : 0) | (fast ? FAST_FLAG : 0);
        blocked = Boolean.FALSE;
        createData(root.getContext().getTimeSource().getCurrentTime());
    }

    protected final void setUnknownStack() {
        flags |= UNKNOWN_STACK_FLAG;
    }

    protected void setPermanentHotspot(boolean full) {
        StackProbeCollector collector = this;

        while (collector != root) {
            if (full) {
                collector.flags &= ~NON_HOTSPOT_FLAG;
                if ((collector.parent.flags & (SAMPLING_FLAG | SAMPLING_ROOT_FLAG)) != 0)
                    collector.flags |= SAMPLING_FLAG;
                if (collector.data == null)
                    collector.init();
            }

            collector.flags |= PERMANENT_HOTSPOT_FLAG;
            collector.blocked = Boolean.FALSE;

            collector = collector.parent;
        }
    }

    protected void setHasMeasurementsInClassifyPeriod() {
        StackProbeCollector collector = this;

        while (collector != root) {
            collector.flags |= HAS_MEASUREMENTS_IN_CLASSIFY_PERIOD_FLAG;
            collector = collector.parent;
        }
    }

    protected StackProbeCollector recoverStack(int index, boolean full) {
        StackProbeRootCollector root = getRoot();
        Container container = root.getContainer();
        ProbeContext context = root.getContext();
        StackProbe probe = root.getProbe();
        List<StackTraceElement> elements = Arrays.asList(container.getStackTrace());
        Collections.reverse(elements);

        StackProbeCollector child = getRoot();
        for (StackTraceElement element : elements) {
            if (context.isProbe(element.getClassName()))
                break;

            List<JoinPointEntry> joinPoints = context.getJoinPointProvider().findJoinPoints(element.getClassName(),
                    element.getMethodName(), ThreadLocalAccessor.underAgent ? AgentStackProbeInterceptor.class :
                            AgentlessStackProbeInterceptor.class);

            JoinPointEntry found = null;
            if (joinPoints.size() > 1) {
                int lineNumber = -1;
                for (JoinPointEntry entry : joinPoints) {
                    if (index != -1 && entry.index == index) {
                        found = entry;
                        break;
                    }

                    if (found == null) {
                        found = entry;
                        lineNumber = entry.joinPoint.getSourceLineNumber();
                    } else if ((element.getLineNumber() >= 0 && entry.joinPoint.getSourceLineNumber() <= element.getLineNumber()) &&
                            entry.joinPoint.getSourceLineNumber() > lineNumber) {
                        found = entry;
                        lineNumber = entry.joinPoint.getSourceLineNumber();
                    }
                }
            } else if (!joinPoints.isEmpty())
                found = joinPoints.get(0);

            if (found == null)
                continue;

            if (probe.isUltraFastMethod(found.joinPoint.getClassName() + "." + found.joinPoint.getMethodSignature()))
                break;

            StackProbeCollector collector = child.getChild(found.index, found.version, probe, null);
            if (collector == null)
                continue;

            child = collector;

            if (full)
                child.blocked = Boolean.FALSE;
        }

        if (full) {
            StackProbeCollector collector = child;
            while (collector != getRoot()) {
                collector.setUnknownStack();
                collector = collector.getParent();
            }
        }

        return child;
    }

    protected StackProbeCollector recoverStackBranch(int index, Object param) {
        StackProbeCollector collector = findChild(index, param);
        if (collector != null && (collector.flags & UNKNOWN_STACK_FLAG) == 0)
            return collector;

        collector = recoverStack(index, false);
        if (collector == null || collector == root)
            return null;

        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.stackBranchRecovered(Meters.shorten(collector.getCallPath())));

        return collector;
    }

    protected ICallPath getStackCallPath(int index) {
        StackProbeRootCollector root = getRoot();
        Container container = root.getContainer();
        ProbeContext context = root.getContext();
        StackProbe probe = root.getProbe();
        List<StackTraceElement> elements = Arrays.asList(container.getStackTrace());
        Collections.reverse(elements);

        ICallPath child = CallPath.root();
        for (StackTraceElement element : elements) {
            if (context.isProbe(element.getClassName()))
                break;

            List<JoinPointEntry> joinPoints = context.getJoinPointProvider().findJoinPoints(element.getClassName(),
                    element.getMethodName(), ThreadLocalAccessor.underAgent ? AgentStackProbeInterceptor.class :
                            AgentlessStackProbeInterceptor.class);

            JoinPointEntry found = null;
            if (joinPoints.size() > 1) {
                int lineNumber = -1;
                for (JoinPointEntry entry : joinPoints) {
                    if (index != -1 && entry.index == index) {
                        found = entry;
                        break;
                    }

                    if (found == null) {
                        found = entry;
                        lineNumber = entry.joinPoint.getSourceLineNumber();
                    } else if ((element.getLineNumber() >= 0 && entry.joinPoint.getSourceLineNumber() <= element.getLineNumber()) &&
                            entry.joinPoint.getSourceLineNumber() > lineNumber) {
                        found = entry;
                        lineNumber = entry.joinPoint.getSourceLineNumber();
                    }
                }
            } else if (!joinPoints.isEmpty())
                found = joinPoints.get(0);

            if (found == null)
                continue;

            if (probe.isUltraFastMethod(found.joinPoint.getClassName() + "." + found.joinPoint.getMethodSignature()))
                break;

            IMetricName metric = MetricName.get(found.joinPoint.getClassName() + "." + found.joinPoint.getMethodSignature());
            child = child.getChild(metric);
        }

        return child;
    }

    protected void addChildrenTotalDelta(long value) {
        if (data != null)
            data.childrenTotalDelta += value;
    }

    private boolean initSampleCount(double tolerableOverhead, long selfOverhead, StackProbeCalibrateInfo info,
                                    long estimationPeriod) {
        double ratio = 0, onSampleCount = 0, offSampleCount = 0;
        if (tolerableOverhead > selfOverhead) {
            ratio = (tolerableOverhead - selfOverhead) / data.totalChildrenOverhead;
            onSampleCount = data.count * ratio * 10000000d / estimationPeriod;
            offSampleCount = data.count * 10000000d / estimationPeriod - onSampleCount;

            onSampleCount = Math.ceil(onSampleCount);
            offSampleCount = Math.ceil(onSampleCount * (1 - ratio) / ratio);
            ratio = offSampleCount / onSampleCount;
        }

        if (data.count > 100 &&
                (onSampleCount == 0 || (root.getConfiguration().getTolerableOverhead() > 1 && root.getConfiguration().getTolerableOverhead() < 100))) {
            if (data.count > 20000) {
                if (ratio < 500) {
                    onSampleCount = 1;
                    offSampleCount = 500;
                }
            } else if (data.count > 1000) {
                if (ratio < 100) {
                    onSampleCount = 1;
                    offSampleCount = 100;
                }
            } else if (ratio < 10) {
                onSampleCount = 1;
                offSampleCount = 10;
            }
        }

        if (ratio > StackProbeCalibrateInfo.MAX_SAMPLING_RATIO) {
            onSampleCount = 1;
            offSampleCount = StackProbeCalibrateInfo.MAX_SAMPLING_RATIO;
        }

        if (onSampleCount == 0)
            return false;

        data.onSampleCount = (long) onSampleCount;
        data.offSampleCount = (long) offSampleCount;
        data.sampleCount = (long) onSampleCount;
        blocked = Boolean.FALSE;

        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, marker, messages.samplingRootSet(tolerableOverhead, selfOverhead, data.totalChildrenOverhead,
                    data.onSampleCount, data.offSampleCount));

        return true;
    }

    private final void updateSampleCount() {
        if (data.onSampleCount == 0)
            return;

        data.sampleCount--;
        if (blocked == null) {
            if (data.sampleCount <= 0) {
                data.sampleCount = data.onSampleCount;
                blocked = Boolean.FALSE;
            }
        } else {
            if (data.sampleCount <= 0) {
                data.sampleCount = data.offSampleCount;
                blocked = null;
            }
        }
    }

    private final void setChildrenSamplingFlag() {
        for (StackProbeCollector collector = firstChild; collector != null; collector = collector.nextSibling) {
            collector.flags &= ~SAMPLING_ROOT_FLAG;

            if ((collector.flags & NON_HOTSPOT_FLAG) != 0)
                continue;

            collector.flags |= SAMPLING_FLAG;
            if (collector.data != null) {
                collector.data.sampleCount = 0;
                collector.data.onSampleCount = 0;
                collector.data.offSampleCount = 0;
            }
            if ((collector.flags & HOTSPOT_FLAG) != 0)
                collector.blocked = Boolean.FALSE;
            else
                collector.blocked = null;

            collector.setChildrenSamplingFlag();
        }
    }

    private final void clearUnknownStackFlag() {
        flags &= ~UNKNOWN_STACK_FLAG;
        for (StackProbeCollector collector = firstChild; collector != null; collector = collector.nextSibling) {
            if ((collector.flags & UNKNOWN_STACK_FLAG) != 0)
                collector.clearUnknownStackFlag();
        }
    }

    private final void beginValueToTicks(long currentCpuTime) {
        if ((flags & FAST_FLAG) != 0)
            return;

        if (data.beginValue == 0)
            data.beginValue = Times.getTickCount();
        else {
            data.beginValue = Times.getTickCount() - (long) ((currentCpuTime - data.beginValue) * Times.getTickFrequency());
            if (data.beginValue < 0)
                data.beginValue = 0;
        }
    }

    private final void beginValueToMillis(long currentCpuTime) {
        if ((flags & FAST_FLAG) == 0)
            return;

        if (data.beginValue == 0)
            data.beginValue = currentCpuTime;
        else {
            data.beginValue = currentCpuTime - (long) ((Times.getTickCount() - data.beginValue) / Times.getTickFrequency());
            if (data.beginValue < 0)
                data.beginValue = 0;
        }
    }

    protected long getNormalCollectorFullOuterOverhead(boolean classify) {
        StackProbeCalibrateInfo info = root.getCalibrateInfo();
        return info.normalCollectorFullOuterOverhead;
    }

    protected long getOuterOverhead() {
        StackProbeCalibrateInfo info = root.getCalibrateInfo();
        if ((flags & HOTSPOT_FLAG) != 0) {
            if ((flags & FAST_FLAG) != 0)
                return info.fastCollectorFullOuterOverhead;
            else
                return info.normalCollectorFullOuterOverhead;
        } else {
            if ((flags & FAST_FLAG) != 0)
                return info.fastCollectorEstimatingOuterOverhead;
            else
                return info.normalCollectorEstimatingOuterOverhead;
        }
    }

    protected boolean hasOutliers() {
        return true;
    }

    protected boolean allowConcurrency() {
        return true;
    }

    protected void setParentNonSamplingRoot() {
        StackProbeCollector parent = this.parent;
        while (parent != root) {
            parent.flags |= NON_SAMPLING_ROOT_FLAG;
            parent = parent.parent;
        }
    }

    private void incrementTotalChildrenOverhead(long overheadDelta) {
        if (overheadDelta == 0)
            return;

        StackProbeCollector parent = this.parent;
        while (parent != root && parent.data != null) {
            parent.data.totalChildrenOverhead += overheadDelta;
            if (parent.data.totalChildrenOverhead < 0)
                parent.data.totalChildrenOverhead = 0;

            parent = parent.parent;
        }
    }

    private boolean checkUltraFast() {
        if (getClass() == StackProbeCollector.class &&
                data.count > 1000 && data.total / data.count < root.getConfiguration().getUltraFastMethodThreshold()) {
            String name = callPath.getLastSegment().toString();
            int pos = name.lastIndexOf('.');
            String className;
            if (pos != -1)
                className = name.substring(0, pos);
            else
                className = "";
            root.addUltraFastMethod(name, className, index, data.count, data.total / data.count);
            return true;
        } else
            return false;
    }

    private double getParentApproximationMultiplier() {
        double approximationMultiplier = 1;
        if ((flags & SAMPLING_FLAG) != 0) {
            StackProbeCollector parent = this.parent;
            while (parent != root) {
                if ((parent.flags & SAMPLING_ROOT_FLAG) != 0) {
                    approximationMultiplier = parent.data.onSampleCount > 0 ? (double) (parent.data.onSampleCount +
                            parent.data.offSampleCount) / parent.data.onSampleCount : 1;
                    break;
                }
                parent = parent.parent;
            }
        }
        return approximationMultiplier;
    }

    private void setParentHotspot() {
        StackProbeCollector parent = this.parent;
        while (parent != root) {
            parent.flags |= HOTSPOT_FLAG;
            parent.blocked = Boolean.FALSE;
            parent = parent.parent;
        }
    }

    private final void setNonHotspot() {
        flags = (flags & BEGIN_BLOCKED_FLAG) | NON_HOTSPOT_FLAG;
        blocked = null;

        for (StackProbeCollector collector = firstChild; collector != null; collector = collector.nextSibling)
            collector.setNonHotspot();

        clearData();
    }

    private boolean hasChildrenHotspots() {
        for (StackProbeCollector collector = firstChild; collector != null; collector = collector.nextSibling) {
            if ((collector.flags & HOTSPOT_FLAG) != 0)
                return true;
        }
        return false;
    }

    private void checkStack(StackProbeCollector collector) {
        if (!collector.getRoot().getScope().isPermanent())
            return;

        ICallPath callPath = getStackCallPath(collector.index);
        if (!collector.getCallPath().equals(callPath)) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, marker, messages.stackCorrupted(collector.getCallPath(), callPath));
        }
    }

    private static class StackProbeCollectorData {
        private IFieldCollector[] inherentFieldCollectors;
        private IFieldCollector[] totalFieldCollectors;
        private MeterContainer meters;
        private List<StackCounter> stackCounters;
        private IGauge concurrencyLevel;
        private boolean concurrencyLevelEnabled;
        private long startEstimationScopeTime;
        private long startEstimationRootTotalOverhead;
        private long beginValue;
        private long beginTotalOverhead;
        private long beginSelfOverheadTickCount;
        private long childrenTotalDelta;
        private long totalChildrenOverhead;
        private long measurementCount;
        private long inherent;
        private long total;
        private long samplingTotal;
        private long count;
        private long outliersCount;
        private long sampleCount;
        private long onSampleCount;
        private long offSampleCount;
    }

    private interface IMessages {
        @DefaultMessage("Sampling root is set. tolerable overhead: {0}, self overhead: {1}, total children overhead: {2}, on sample count: {3}, off sample count: {4}")
        ILocalizedMessage samplingRootSet(double tolerableOverhead, long selfOverhead, long totalChildrenOverhead,
                                          long onSampleCount, long offSampleCount);

        @DefaultMessage("Non-hotspot is detected. count: {0}, total: {1}, approximation multiplier: {2}, estimation period: {3}, total overhead: {4}, coverage: {5}")
        ILocalizedMessage nonHotspotDetected(long count, long total, double approximationMultiplier, long estimationPeriod,
                                             long totalOverhead, double coverage);

        @DefaultMessage("Hotspot is detected. count: {0}, total: {1}, approximation multiplier: {2}, estimation period: {3}, total overhead: {4}, coverage: {5}")
        ILocalizedMessage hotspotDetected(long count, long total, double approximationMultiplier, long estimationPeriod,
                                          long totalOverhead, double coverage);

        @DefaultMessage("Permanent hotspot is detected.")
        ILocalizedMessage permanentHotspotDetected();

        @DefaultMessage("Hotspot estimation is started. estimation time: {0}")
        ILocalizedMessage hotspotEstimationStarted(long startEstimationScopeTime);

        @DefaultMessage("Stack branch is recovered: {0}.")
        ILocalizedMessage stackBranchRecovered(String collector);

        @DefaultMessage("Collector is removed: {0}.")
        ILocalizedMessage collectorRemoved(String collector);

        @DefaultMessage("Stack is corrupted. \n    Existing: {0}\n    Expected: {1}")
        ILocalizedMessage stackCorrupted(ICallPath existing, ICallPath expected);

        @DefaultMessage("Stack top is changed. \n    Old: {0}\n    New: {1}")
        ILocalizedMessage topChanged(StackProbeCollector oldTop, StackProbeCollector newTop);
    }
}
